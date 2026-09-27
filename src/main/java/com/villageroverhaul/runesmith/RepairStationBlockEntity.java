package com.villageroverhaul.runesmith;

import com.villageroverhaul.client.ui.ModMenuTypes;
import com.villageroverhaul.station.ThreeInThreeOutMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The repair station's inventory (Runesmith passive, 2026-09-26): slots 0-2 take damaged items, 3-5 hold
 * the repaired ones. Hopper rules like the crushing station: from above or the sides into the input (only
 * damaged, damageable items), a hopper below pulls only from the output. Repairing happens when the owning
 * Runesmith works here (passive/RepairWork calls repairStep): free, no XP, a fixed number of durability
 * points per step - cheap items are done fast, diamond gear takes days (Runesmith.md).
 */
public class RepairStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int INPUT_SLOTS = ThreeInThreeOutMenu.INPUT_SLOTS;
    public static final int SLOTS = ThreeInThreeOutMenu.SLOTS;
    /** Ticks between two grindstone sounds while the Runesmith fills its passive meter here (Tweak-Werte.md). */
    public static final int WORK_SOUND_INTERVAL = 20;
    /** Volume of that sound (Tweak-Werte.md). */
    public static final float WORK_SOUND_VOLUME = 0.5F;
    private static final int[] INPUT = {0, 1, 2};
    private static final int[] OUTPUT = {3, 4, 5};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** Game time until which the station counts as worked - not saved, the next work scan renews it. */
    private long workedUntil;
    /** Passive rank of the owning Runesmith as its work scan last reported it; -1 = none yet. Saved. */
    private int ownerRank = -1;

    public RepairStationBlockEntity(BlockPos pos, BlockState state) {
        super(RunesmithBlockEntities.REPAIR_STATION.get(), pos, state);
    }

    public static boolean isRepairable(ItemStack stack) {
        return stack.isDamageableItem() && stack.isDamaged();
    }

    /** Damaged, or - only for an owner on the padding rank - still missing padding. */
    private boolean needsWork(ItemStack stack) {
        return isRepairable(stack) || addsPadding() && stack.isDamageableItem() && BonusDurability.of(stack) < BonusDurability.max(stack);
    }

    private boolean addsPadding() {
        return ownerRank >= BonusDurability.FROM_PASSIVE_RANK;
    }

    /**
     * Called by the owning Runesmith's work scan with its passive rank - on the highest rank the station
     * also tops up padding (BonusDurability). Items that need nothing more at this rank move to the output.
     */
    public void markOwner(int passiveRank) {
        if (passiveRank != ownerRank) {
            ownerRank = passiveRank;
            setChanged();
        }
        if (moveFinishedToOutput()) {
            setChanged();
        }
    }

    /** Called by the owning Runesmith's work scan while it fills its passive meter right here (RepairWork). */
    public void markWorked(long untilGameTime) {
        workedUntil = untilGameTime;
    }

    /** Every WORK_SOUND_INTERVAL ticks while worked: the grindstone sound. */
    void serverTick(ServerLevel level, BlockPos pos) {
        long time = level.getGameTime();
        if (time < workedUntil && time % WORK_SOUND_INTERVAL == 0) {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, WORK_SOUND_VOLUME, 1.0F);
        }
    }

    /** Something in the input still needs repairing (or padding). */
    public boolean hasWork() {
        for (int slot : INPUT) {
            if (needsWork(items.get(slot))) {
                return true;
            }
        }
        return false;
    }

    /**
     * One repair step: up to points on the first input item that needs work (slot order) - durability
     * first; on the padding rank what is left goes into padding, up to BonusDurability.max. A finished item
     * moves to a free output slot right away; with the output full it waits in the input. False if there
     * was nothing to do.
     */
    public boolean repairStep(int points) {
        boolean worked = false;
        for (int slot : INPUT) {
            ItemStack stack = items.get(slot);
            if (needsWork(stack)) {
                int repaired = Math.min(points, stack.getDamageValue());
                stack.setDamageValue(stack.getDamageValue() - repaired);
                if (addsPadding()) {
                    int padding = BonusDurability.of(stack);
                    BonusDurability.set(stack, padding + Math.min(points - repaired, BonusDurability.max(stack) - padding));
                }
                worked = true;
                break;
            }
        }
        if (moveFinishedToOutput() || worked) {
            setChanged();
        }
        return worked;
    }

    private boolean moveFinishedToOutput() {
        boolean moved = false;
        for (int in : INPUT) {
            ItemStack stack = items.get(in);
            if (stack.isEmpty() || needsWork(stack)) {
                continue;
            }
            for (int out : OUTPUT) {
                if (items.get(out).isEmpty()) {
                    items.set(out, stack);
                    items.set(in, ItemStack.EMPTY);
                    moved = true;
                    break;
                }
            }
        }
        return moved;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** Only input slots take items: damaged ones, or undamaged ones still missing padding (they pass straight through below the padding rank). */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < INPUT_SLOTS && stack.isDamageableItem() && (stack.isDamaged() || BonusDurability.of(stack) < BonusDurability.max(stack));
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
        return new ThreeInThreeOutMenu(ModMenuTypes.REPAIR_STATION_MENU.get(), containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        ownerRank = input.getIntOr("owner_rank", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("owner_rank", ownerRank);
    }
}
