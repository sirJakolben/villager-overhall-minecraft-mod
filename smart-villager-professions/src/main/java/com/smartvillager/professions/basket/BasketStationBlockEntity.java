package com.smartvillager.professions.basket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The basket's 5 slots (Farmer harvest basket, Fisherman fish basket) in Vanilla's hopper window - the same layout as the Librarian's
 * enhancement station. The villager fills it (deposit); players put things in and out by hand, a hopper below pulls everything out -
 * nothing flows in from a hopper above or beside, so the basket only holds what the villager made (and what a player adds by hand).
 *
 * The block shows how full it is, by occupied slots (BasketStationBlock.FILL, like the composter): 0 to 5, every slot with anything in it
 * counts one - so five different items fill the basket, whatever their count.
 */
public class BasketStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int SLOTS = 5;
    private static final int[] ALL = {0, 1, 2, 3, 4};
    private static final int[] NONE = {};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public BasketStationBlockEntity(BlockPos pos, BlockState state) {
        super(BasketBlockEntities.BASKET_STATION.get(), pos, state);
    }

    /** Something can still go in: an empty slot or a stack with room. Without it the villager does not walk here. */
    public boolean hasSpace() {
        for (ItemStack stack : items) {
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Puts the stacks in, merging into matching stacks first. What does not fit falls out in front of the basket - the step is done, nothing
     * is held back or lost.
     */
    public void deposit(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemStack rest = stack.copy();
            for (ItemStack there : items) {
                if (!there.isEmpty() && ItemStack.isSameItemSameComponents(there, rest)) {
                    int moved = Math.min(rest.getCount(), there.getMaxStackSize() - there.getCount());
                    there.grow(moved);
                    rest.shrink(moved);
                }
            }
            for (int slot = 0; slot < SLOTS && !rest.isEmpty(); slot++) {
                if (items.get(slot).isEmpty()) {
                    items.set(slot, rest.split(Math.min(rest.getCount(), rest.getMaxStackSize())));
                }
            }
            if (!rest.isEmpty() && level != null) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, rest);
            }
        }
        setChanged();
    }

    /** Every change of the contents: the block shows the number of occupied slots. */
    @Override
    public void setChanged() {
        super.setChanged();
        Level level = this.level;
        if (level == null || level.isClientSide()) {
            return;
        }
        int filled = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                filled++;
            }
        }
        BlockState state = getBlockState();
        if (state.hasProperty(BasketStationBlock.FILL) && state.getValue(BasketStationBlock.FILL) != filled) {
            level.setBlock(worldPosition, state.setValue(BasketStationBlock.FILL, filled), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    /** A hopper below pulls from every slot; from any other face the basket is closed. */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? ALL : NONE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
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
        return new HopperMenu(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }
}
