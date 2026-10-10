package com.villageroverhaul.traderework.mapping;

import com.villageroverhaul.traderework.cartographer.CartographerItems;
import com.villageroverhaul.traderework.client.ui.TradeReworkMenus;
import com.villageroverhaul.traderework.station.ThreeInThreeOutMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The inventory of the Cartographer's adventure station and the Veteran's challenge station (2026-10-10): slots 0-2 take blank maps
 * (adventure map / challenge map - only the kind of this station), slots 3-5 hold the finished maps. Hopper rules like the crushing
 * station: from above or the sides only maps go in, a hopper below pulls only from the output; players may put anything into the
 * output. The drawing itself happens when the owning villager works here (MappingWork calls draw): one blank map is used up and a
 * filled one with a drawn target (MapTargets) lands in the output.
 */
public class MapStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int INPUT_SLOTS = ThreeInThreeOutMenu.INPUT_SLOTS;
    public static final int SLOTS = ThreeInThreeOutMenu.SLOTS;
    /** Ticks to wait after a draw that found no structure in reach before the next one - the search is expensive (one minute). */
    public static final int RETRY_TICKS = 1200;
    private static final int[] INPUT = {0, 1, 2};
    private static final int[] OUTPUT = {3, 4, 5};

    private final MapPool.Kind kind;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** Game time before which no new draw starts - not saved, a reload simply tries again. */
    private long retryAt;

    public MapStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MapPool.Kind kind) {
        super(type, pos, state);
        this.kind = kind;
    }

    public MapPool.Kind kind() {
        return kind;
    }

    private Item blankMap() {
        return (kind == MapPool.Kind.ADVENTURE ? CartographerItems.ADVENTURE_MAP : CartographerItems.CHALLENGE_MAP).get();
    }

    private String filledMapName() {
        return "filled_map.vo_trade_rework." + kind.getSerializedName();
    }

    public static boolean isInput(int slot) {
        return slot < INPUT_SLOTS;
    }

    /** A blank map waits in the input, a free output slot is there for the result, and the last draw did not fail just now. */
    public boolean hasWork(ServerLevel level) {
        return level.getGameTime() >= retryAt && firstInput() >= 0 && firstFreeOutput() >= 0;
    }

    /**
     * Draws one map: a target from the pool (MapTargets, up to MAX_SEARCHES searches), then the blank map becomes the filled one. Returns
     * false when no structure was in reach - the blank map stays and the next try is RETRY_TICKS away.
     */
    public boolean draw(ServerLevel level) {
        int input = firstInput();
        int output = firstFreeOutput();
        if (input < 0 || output < 0) {
            return false;
        }
        Optional<ItemStack> map = MapTargets.draw(level, kind, getBlockPos(), level.getRandom(), filledMapName());
        if (map.isEmpty()) {
            retryAt = level.getGameTime() + RETRY_TICKS;
            return false;
        }
        items.get(input).shrink(1);
        items.set(output, map.get());
        setChanged();
        return true;
    }

    private int firstInput() {
        for (int slot : INPUT) {
            if (!items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private int firstFreeOutput() {
        for (int slot : OUTPUT) {
            if (items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !isInput(slot) || stack.is(blankMap());
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT : INPUT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isInput(slot) && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isInput(slot);
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
        return new ThreeInThreeOutMenu((kind == MapPool.Kind.ADVENTURE ? TradeReworkMenus.ADVENTURE_STATION_MENU : TradeReworkMenus.CHALLENGE_STATION_MENU).get(),
                containerId, inventory, this);
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
