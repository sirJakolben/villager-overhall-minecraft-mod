package com.smartvillager.professions.station;

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

    private final Container station;
    private final int inputSlots;
    private final int outputSlots;

    /** Client side: an empty stand-in container, synced from the server. */
    public ThreeInThreeOutMenu(MenuType<?> type, int containerId, Inventory inventory) {
        this(type, containerId, inventory, new SimpleContainer(SLOTS), INPUT_SLOTS, INPUT_SLOTS);
    }

    public ThreeInThreeOutMenu(MenuType<?> type, int containerId, Inventory inventory, Container station) {
        this(type, containerId, inventory, station, INPUT_SLOTS, INPUT_SLOTS);
    }

    /** Client side of a station with its own slot counts (the challenge station: 1 + 4). */
    public ThreeInThreeOutMenu(MenuType<?> type, int containerId, Inventory inventory, int inputSlots, int outputSlots) {
        this(type, containerId, inventory, new SimpleContainer(inputSlots + outputSlots), inputSlots, outputSlots);
    }

    public ThreeInThreeOutMenu(MenuType<?> type, int containerId, Inventory inventory, Container station, int inputSlots, int outputSlots) {
        super(type, containerId);
        this.station = station;
        this.inputSlots = inputSlots;
        this.outputSlots = outputSlots;
        checkContainerSize(station, inputSlots + outputSlots);
        station.startOpen(inventory.player);
        for (int slot = 0; slot < inputSlots + outputSlots; slot++) {
            int column = columnOf(slot);
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

    public int inputSlots() {
        return inputSlots;
    }

    public int outputSlots() {
        return outputSlots;
    }

    /** First row column of the input: 0 for 3 + 3 slots, one column in for fewer so the row stays centred. */
    public int firstInputColumn() {
        return inputSlots >= 3 ? 0 : 1;
    }

    /** First row column of the output slots (the row has 9 columns, the layout mirrors left to right). */
    public int firstOutputColumn() {
        return 9 - outputSlots - firstInputColumn();
    }

    public int columnOf(int slot) {
        return slot < inputSlots ? firstInputColumn() + slot : firstOutputColumn() + slot - inputSlots;
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
            } else if (!moveItemStackTo(stack, 0, inputSlots, false)) {
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
