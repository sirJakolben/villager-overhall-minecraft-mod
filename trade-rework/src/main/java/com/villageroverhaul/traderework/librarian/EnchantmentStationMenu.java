package com.villageroverhaul.traderework.librarian;

import com.villageroverhaul.traderework.client.ui.TradeReworkMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Vanilla's HopperMenu layout (5 slots in a row, same positions) with slots that only take books the
 * station accepts. Needs its own menu type because HopperMenu's slots accept anything; the screen just
 * draws the Vanilla hopper texture (client/ui/EnchantmentStationScreen).
 */
public class EnchantmentStationMenu extends AbstractContainerMenu {

    private final Container station;

    /** Client side: an empty stand-in container, synced from the server. */
    public EnchantmentStationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(EnchantmentStationBlockEntity.SLOTS));
    }

    public EnchantmentStationMenu(int containerId, Inventory inventory, Container station) {
        super(TradeReworkMenus.ENCHANTMENT_STATION_MENU.get(), containerId);
        this.station = station;
        checkContainerSize(station, EnchantmentStationBlockEntity.SLOTS);
        station.startOpen(inventory.player);
        for (int x = 0; x < EnchantmentStationBlockEntity.SLOTS; x++) {
            addSlot(new Slot(station, x, 44 + x * 18, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return BookUpgrades.isUpgradeable(stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 51);
    }

    @Override
    public boolean stillValid(Player player) {
        return station.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            int stationSize = station.getContainerSize();
            if (slotIndex < stationSize) {
                if (!moveItemStackTo(stack, stationSize, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, stationSize, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return clicked;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        station.stopOpen(player);
    }
}
