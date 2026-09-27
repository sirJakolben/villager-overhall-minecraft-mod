package com.villageroverhaul.traderework.salvager;

import com.villageroverhaul.traderework.client.ui.TradeReworkMenus;
import com.villageroverhaul.traderework.TradeReworkRegistries;
import com.villageroverhaul.traderework.runesmith.UpgradeTemplates;
import com.villageroverhaul.traderework.station.ThreeInThreeOutMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The smelting station's inventory (Salvager passive, Obsidian Salvager.md, 2026-09-27): slots 0-2 take metal
 * gear (only what a salvage rule lists - no diamond, wood, stone, leather), 3-5 collect what comes out.
 * Hopper rules like the crushing station: from above or the sides into the input, a hopper below pulls only
 * from the output. Smelting happens when the owning Salvager works here (passive/SmeltingWork calls
 * smeltStep): one piece per step.
 *
 * Yield (payout): the rule's full amount x the rank share (SHARE_BY_RANK) x the piece's condition (durability
 * left), rounded down, at least 1 - so never nuggets for ingot gear, and a battered piece still gives one
 * ingot, more than Vanilla's furnace. Netherite doesn't melt: it comes back as the diamond piece (everything
 * kept) plus scraps by the same formula. A step only runs if everything fits into the output; otherwise the
 * piece waits.
 */
public class SmeltingStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int INPUT_SLOTS = ThreeInThreeOutMenu.INPUT_SLOTS;
    public static final int SLOTS = ThreeInThreeOutMenu.SLOTS;
    /** Share of the full yield by passive rank 0..6 (Tweak-Werte.md) - the top rank gives the whole recipe back. */
    private static final int[] SHARE_PERCENT_BY_RANK = {25, 35, 45, 55, 70, 85, 100};
    /** Ticks between two furnace sounds while the Salvager fills its passive meter here (Tweak-Werte.md). */
    public static final int WORK_SOUND_INTERVAL = 20;
    public static final float WORK_SOUND_VOLUME = 0.6F;
    private static final int[] INPUT = {0, 1, 2};
    private static final int[] OUTPUT = {3, 4, 5};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private long workedUntil;

    public SmeltingStationBlockEntity(BlockPos pos, BlockState state) {
        super(SalvagerBlockEntities.SMELTING_STATION.get(), pos, state);
    }

    public static Optional<SalvageRule> ruleFor(Level level, ItemStack stack) {
        for (SalvageRule rule : level.registryAccess().lookupOrThrow(TradeReworkRegistries.SALVAGE)) {
            if (rule.items().containsKey(stack.getItem())) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    public static int sharePercent(int passiveRank) {
        return SHARE_PERCENT_BY_RANK[Mth.clamp(passiveRank, 0, SHARE_PERCENT_BY_RANK.length - 1)];
    }

    /** Full amount x rank share x condition, rounded down, at least 1. */
    static int payout(int fullAmount, int passiveRank, ItemStack piece) {
        double condition = piece.isDamageableItem() ? 1.0 - piece.getDamageValue() / (double) piece.getMaxDamage() : 1.0;
        return Math.max(1, (int) Math.floor(fullAmount * sharePercent(passiveRank) / 100.0 * condition));
    }

    public void markWorked(long untilGameTime) {
        workedUntil = untilGameTime;
    }

    /** Every WORK_SOUND_INTERVAL ticks while worked: the blast furnace crackle. */
    void serverTick(ServerLevel level, BlockPos pos) {
        long time = level.getGameTime();
        if (time < workedUntil && time % WORK_SOUND_INTERVAL == 0) {
            level.playSound(null, pos, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, WORK_SOUND_VOLUME, 1.0F);
        }
    }

    public boolean hasWork() {
        if (level == null) {
            return false;
        }
        for (int slot : INPUT) {
            if (!items.get(slot).isEmpty() && ruleFor(level, items.get(slot)).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Smelts the first input piece whose results fit into the output (slot order). False if nothing could be
     * smelted - empty input or the output too full.
     */
    public boolean smeltStep(int passiveRank) {
        if (level == null) {
            return false;
        }
        for (int slot : INPUT) {
            ItemStack piece = items.get(slot);
            Optional<SalvageRule> rule = piece.isEmpty() ? Optional.empty() : ruleFor(level, piece);
            if (rule.isEmpty()) {
                continue;
            }
            List<ItemStack> results = results(rule.get(), piece, passiveRank);
            if (fitsIntoOutput(results)) {
                results.forEach(this::addToOutput);
                items.set(slot, ItemStack.EMPTY);
                setChanged();
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> results(SalvageRule rule, ItemStack piece, int passiveRank) {
        List<ItemStack> results = new ArrayList<>();
        Item downgrade = rule.downgrade().get(piece.getItem());
        if (downgrade != null) {
            ItemStack kept = piece.transmuteCopy(downgrade, 1);
            UpgradeTemplates.rescaleDurability(piece, kept);
            results.add(kept);
        }
        results.add(new ItemStack(rule.result(), payout(rule.items().get(piece.getItem()), passiveRank, piece)));
        return results;
    }

    private boolean fitsIntoOutput(List<ItemStack> results) {
        NonNullList<ItemStack> copy = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        for (int out : OUTPUT) {
            copy.set(out, items.get(out).copy());
        }
        for (ItemStack result : results) {
            if (!add(copy, result.copy())) {
                return false;
            }
        }
        return true;
    }

    private void addToOutput(ItemStack result) {
        add(items, result);
    }

    /** Merges into matching output stacks, then empty output slots; false if something is left over. */
    private static boolean add(NonNullList<ItemStack> slots, ItemStack stack) {
        for (int out : OUTPUT) {
            ItemStack there = slots.get(out);
            if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, stack)) {
                int moved = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                there.grow(moved);
                stack.shrink(moved);
            }
            if (stack.isEmpty()) {
                return true;
            }
        }
        for (int out : OUTPUT) {
            if (slots.get(out).isEmpty()) {
                slots.set(out, stack);
                return true;
            }
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    /** Only input slots take items, and only gear a salvage rule lists. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < INPUT_SLOTS && level != null && ruleFor(level, stack).isPresent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT : INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= INPUT_SLOTS;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ThreeInThreeOutMenu(TradeReworkMenus.SMELTING_STATION_MENU.get(), containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }
}
