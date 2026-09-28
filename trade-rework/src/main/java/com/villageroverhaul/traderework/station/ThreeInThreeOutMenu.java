package com.villageroverhaul.traderework.station;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The window of every passive station with 3 input and 3 output slots (Mason's crushing station, Repair
 * Smith's repair station), laid out like a one-row chest (Vanilla's generic_54 texture, ChestMenu
 * positions): input on the left of the row, output on the right, an arrow in between
 * (client/ui/ThreeInThreeOutScreen). Input slots take what the station's container accepts
 * (canPlaceItem - checked on the server), output slots likewise (the crushing station takes anything there, the others nothing). Each station registers its own menu type
 * with this class (client/ui/ModMenuTypes).
 */
public class ThreeInThreeOutMenu extends AbstractContainerMenu {

    public static final int INPUT_SLOTS = 3;
    public static final int SLOTS = INPUT_SLOTS * 2;
    /** Row columns of the output slots - the input uses columns 0-2. */
    private static final int OUTPUT_FIRST_COLUMN = 6;

    private final Container station;

    /** Client side: an empty stand-in container, synced from the server. */
    public ThreeInThreeOutMenu(MenuType<?> type, int containerId, Inventory inventory) {
        this(type, containerId, inventory, new SimpleContainer(SLOTS));
    }

    public ThreeInThreeOutMenu(MenuType<?> type, int containerId, Inventory inventory, Container station) {
        super(type, containerId);
        this.station = station;
        checkContainerSize(station, SLOTS);
        station.startOpen(inventory.player);
        for (int slot = 0; slot < SLOTS; slot++) {
            int column = isInput(slot) ? slot : OUTPUT_FIRST_COLUMN + slot - INPUT_SLOTS;
            int index = slot;
            addSlot(new Slot(station, slot, 8 + column * 18, 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return station.canPlaceItem(index, stack);
                }
            });
        }
        addStandardInventorySlots(inventory, 8, 49);
    }

    public static boolean isInput(int slot) {
        return slot < INPUT_SLOTS;
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
            } else if (!moveItemStackTo(stack, 0, INPUT_SLOTS, false)) {
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
