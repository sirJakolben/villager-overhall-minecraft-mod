package com.villageroverhaul.librarian;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

/**
 * The enchantment station's inventory: 5 single-item slots like a hopper, a plain Vanilla Container, so
 * hoppers and droppers fill it and comparators read it with no extra code. Contents drop when it breaks
 * (Vanilla does that for every Container block entity).
 *
 * Only books the passive could ever upgrade go in (BookUpgrades.isUpgradeable). A hopper below only pulls
 * out books the owning villager can't upgrade any further at its current rank - so a hopper chain in,
 * station, hopper out works like a furnace. The owner's rank is reported by its work scan (markOwner);
 * without a recent report nobody upgrades here, so everything may be pulled out. Players take anything.
 */
public class EnchantmentStationBlockEntity extends BaseContainerBlockEntity {

    public static final int SLOTS = 5;
    /** A report older than this means the station has no working owner (the scan reports every 100 ticks). */
    private static final long OWNER_TIMEOUT_TICKS = 250;
    private static final int NO_OWNER = -1;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int ownerRank = NO_OWNER;
    private long ownerSeenAt;

    public EnchantmentStationBlockEntity(BlockPos pos, BlockState state) {
        super(LibrarianBlockEntities.ENCHANTMENT_STATION.get(), pos, state);
    }

    /** Called by the owning villager's work scan: it owns this station and has this passive rank. */
    public void markOwner(int passiveRank) {
        if (level == null) {
            return;
        }
        if (passiveRank != ownerRank) {
            ownerRank = passiveRank;
            setChanged();
        }
        ownerSeenAt = level.getGameTime();
    }

    private Optional<Integer> ownerRank() {
        boolean recent = level != null && ownerRank != NO_OWNER && level.getGameTime() - ownerSeenAt <= OWNER_TIMEOUT_TICKS;
        return recent ? Optional.of(ownerRank) : Optional.empty();
    }

    public boolean hasUpgradeableBook(int passiveRank) {
        return items.stream().anyMatch(stack -> BookUpgrades.nextUpgrade(stack, passiveRank).isPresent());
    }

    /**
     * Upgrades the lowest-level enchantment across all books that can still be upgraded at this rank
     * (ties: slot order = arrival order); false if none can.
     */
    public boolean upgradeNextBook(int passiveRank) {
        ItemStack lowestBook = null;
        Holder<Enchantment> lowestEnchantment = null;
        int lowestLevel = Integer.MAX_VALUE;
        for (ItemStack stack : items) {
            var enchantment = BookUpgrades.nextUpgrade(stack, passiveRank);
            if (enchantment.isPresent()) {
                int level = EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment.get());
                if (level < lowestLevel) {
                    lowestBook = stack;
                    lowestEnchantment = enchantment.get();
                    lowestLevel = level;
                }
            }
        }
        if (lowestBook == null) {
            return false;
        }
        BookUpgrades.upgrade(lowestBook, lowestEnchantment);
        setChanged();
        return true;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return BookUpgrades.isUpgradeable(stack);
    }

    @Override
    public boolean canTakeItem(Container into, int slot, ItemStack stack) {
        return ownerRank().map(rank -> BookUpgrades.nextUpgrade(stack, rank).isEmpty()).orElse(true);
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
        return new EnchantmentStationMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        ownerRank = input.getIntOr("owner_rank", NO_OWNER);
        ownerSeenAt = input.getLongOr("owner_seen_at", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("owner_rank", ownerRank);
        output.putLong("owner_seen_at", ownerSeenAt);
    }
}
