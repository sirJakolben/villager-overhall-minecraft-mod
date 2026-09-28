package com.villageroverhaul.traderework.mason;

import com.villageroverhaul.traderework.client.ui.TradeReworkMenus;
import com.villageroverhaul.traderework.station.ThreeInThreeOutMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * The crushing station's inventory (Mason passive, 2026-09-26): slots 0-2 are the input, 3-5 the output.
 * Hopper rules like a furnace: from above or the sides items go into the input (only what can be ground),
 * a hopper below pulls only from the output. Players may take from anywhere and put anything into the output (2026-09-28); the input takes only what can be ground.
 * The crushing itself happens when the owning villager works here (CrushingWork calls crushBatch).
 */
public class CrushingStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int INPUT_SLOTS = ThreeInThreeOutMenu.INPUT_SLOTS;
    public static final int SLOTS = ThreeInThreeOutMenu.SLOTS;
    /** Ticks between two tuff hit sounds while the Mason fills its passive meter here - Vanilla's mining rhythm (MultiPlayerGameMode: every 4 ticks). */
    public static final int CHISEL_SOUND_INTERVAL = 4;
    /** Volume of that sound (Tweak-Werte.md) - louder than Vanilla's mining (0.25) on purpose. */
    public static final float CHISEL_SOUND_VOLUME = 0.75F;
    /** Particles of the input rising from the blades per sound tick (Tweak-Werte.md). */
    public static final int BLADE_PARTICLES = 3;
    /** Particles of the result blown out of the exhaust per sound tick (Tweak-Werte.md). */
    public static final int EXHAUST_PARTICLES = 2;
    /** Push out of the exhaust; above ~1 Vanilla's random spread hardly changes the direction any more (Tweak-Werte.md). */
    public static final double EXHAUST_PUSH = 1.5;
    private static final int[] INPUT = {0, 1, 2};
    private static final int[] OUTPUT = {3, 4, 5};

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** Game time until which the station counts as worked - not saved, the next work scan renews it. */
    private long workedUntil;

    public CrushingStationBlockEntity(BlockPos pos, BlockState state) {
        super(MasonBlockEntities.CRUSHING_STATION.get(), pos, state);
    }

    public static boolean isInput(int slot) {
        return slot < INPUT_SLOTS;
    }

    /**
     * Called by the owning Mason's work scan while it fills its passive meter right here (CrushingWork):
     * the station plays the tuff mining sound until the next scan should have renewed it.
     */
    public void markWorked(long untilGameTime) {
        workedUntil = untilGameTime;
    }

    /**
     * Every CHISEL_SOUND_INTERVAL ticks while worked and something can be crushed (2026-09-28 - an empty or
     * full station stays quiet): the tuff hit sound at the pitch Vanilla uses while mining (half the block's
     * pitch), particles of what goes in rising from the blades and of what comes out blowing from the exhaust.
     * The blades turn only while worked (WORKING).
     */
    void serverTick(ServerLevel level, BlockPos pos) {
        long time = level.getGameTime();
        boolean worked = time < workedUntil;
        BlockState state = getBlockState();
        if (state.getValue(CrushingStationBlock.WORKING) != worked) {
            // Only at the start and end of a work phase: the blades model switches between still and animated texture.
            level.setBlock(pos, state.setValue(CrushingStationBlock.WORKING, worked), Block.UPDATE_CLIENTS);
        }
        if (worked && time % CHISEL_SOUND_INTERVAL == 0) {
            nextStep(level, null).ifPresent(step -> {
                level.playSound(null, pos, SoundType.TUFF.getHitSound(), SoundSource.BLOCKS, CHISEL_SOUND_VOLUME, SoundType.TUFF.getPitch() * 0.5F);
                spawnParticles(level, pos, step);
            });
        }
    }

    /**
     * Sent from the server (sendParticles), so the client needs neither the inventory nor the worked time.
     * The input is the first slot this step takes from - the kind the next batch will crush. Blades: a small
     * cloud in the middle 2 px above the blades with no push, so Vanilla's own random start speed (mostly
     * up, then gravity) lets the bits hop about a quarter block and fall back in. Exhaust: the hole in the
     * back face (px 5-9 from the left, 3-4 from the bottom, seen from behind), each particle pushed out backwards.
     */
    private void spawnParticles(ServerLevel level, BlockPos pos, Step step) {
        Item input = null;
        for (int slot : INPUT) {
            if (step.take()[slot] > 0) {
                input = items.get(slot).getItem();
                break;
            }
        }
        if (input == null) {
            return;
        }
        level.sendParticles(particleOf(input), pos.getX() + 0.5, pos.getY() + 12.5 / 16.0, pos.getZ() + 0.5,
                BLADE_PARTICLES, 0.5 / 16.0, 0.0, 0.5 / 16.0, 0.0);
        ParticleOptions output = particleOf(step.output());
        RandomSource random = level.getRandom();
        Direction back = getBlockState().getValue(CrushingStationBlock.FACING).getOpposite();
        Direction left = back.getClockWise();
        for (int i = 0; i < EXHAUST_PARTICLES; i++) {
            double fromLeft = (4 + random.nextFloat() * 5) / 16.0;
            double x = pos.getX() + 0.5 + back.getStepX() * (0.5 + 0.5 / 16.0) + left.getStepX() * (0.5 - fromLeft);
            double z = pos.getZ() + 0.5 + back.getStepZ() * (0.5 + 0.5 / 16.0) + left.getStepZ() * (0.5 - fromLeft);
            // count 0: one particle, the offset becomes its direction - mostly backwards, the client adds a little spread.
            level.sendParticles(output, x, pos.getY() + (2 + random.nextFloat() * 2) / 16.0, z,
                    0, back.getStepX(), 0.0, back.getStepZ(), EXHAUST_PUSH);
        }
    }

    /** Block items give block break particles (sandstone, sand, gravel), all others item particles. */
    private static ParticleOptions particleOf(Item item) {
        return item instanceof BlockItem block
                ? new BlockParticleOption(ParticleTypes.BLOCK, block.getBlock().defaultBlockState())
                : new ItemParticleOption(ParticleTypes.ITEM, item);
    }

    /** One crushing step: how many items to take from each input slot for one output. */
    private record Step(Item output, int[] take) {
    }

    /** Something in the input can be crushed and its result still fits in the output. */
    public boolean hasWork(ServerLevel level) {
        return nextStep(level, null).isPresent();
    }

    /**
     * One batch (2026-09-26): up to batchSize results of one kind only (CrushingWork sets it by passive rank). The kind is that of the first
     * input slot (in slot order) that can go; the batch ends early when that kind runs out or stops
     * fitting - 9 stone and 1 smooth basalt make 4, 4 and 1 cobblestone, then the basalt, each its own
     * batch. Returns the number of results made - 0 means nothing happened.
     */
    public int crushBatch(ServerLevel level, int batchSize) {
        Optional<Step> step = nextStep(level, null);
        if (step.isEmpty()) {
            return 0;
        }
        Item kind = step.get().output();
        int made = 0;
        while (step.isPresent() && made < batchSize) {
            for (int slot : INPUT) {
                items.get(slot).shrink(step.get().take()[slot]);
            }
            insert(new ItemStack(kind));
            made++;
            step = nextStep(level, kind);
        }
        setChanged();
        return made;
    }

    /**
     * The next step, for the first input slot (in slot order) whose result still fits - only of the given
     * kind, if one is given. All input slots with the same result count together (2026-09-26): one slab
     * here and one there, or a polished andesite slab and an andesite slab, make one andesite. Each item is
     * worth 1/inputCount of the result; a step takes items worth exactly one result, the most valuable
     * first (stairs before slabs).
     */
    private Optional<Step> nextStep(ServerLevel level, @Nullable Item onlyKind) {
        Item[] outputs = new Item[INPUT_SLOTS];
        int[] needs = new int[INPUT_SLOTS];
        for (int slot : INPUT) {
            ItemStack input = items.get(slot);
            Optional<CrushingRecipes.Result> result = input.isEmpty() ? Optional.empty() : CrushingRecipes.of(level, input.getItem());
            if (result.isPresent()) {
                outputs[slot] = result.get().output();
                needs[slot] = result.get().inputCount();
            }
        }
        for (int slot : INPUT) {
            if (outputs[slot] != null && (onlyKind == null || outputs[slot] == onlyKind) && fits(new ItemStack(outputs[slot]))) {
                Optional<Step> step = plan(outputs[slot], outputs, needs);
                if (step.isPresent()) {
                    return step;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Step> plan(Item output, Item[] outputs, int[] needs) {
        int units = 1;
        for (int slot : INPUT) {
            if (outputs[slot] == output) {
                units = units / gcd(units, needs[slot]) * needs[slot];
            }
        }
        int[] take = new int[INPUT_SLOTS];
        int remaining = units;
        for (int slot : IntStream.of(INPUT).boxed().sorted(Comparator.comparingInt(s -> needs[s])).mapToInt(Integer::intValue).toArray()) {
            if (outputs[slot] != output) {
                continue;
            }
            int share = units / needs[slot];
            take[slot] = Math.min(items.get(slot).getCount(), remaining / share);
            remaining -= take[slot] * share;
        }
        return remaining == 0 ? Optional.of(new Step(output, take)) : Optional.empty();
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    private boolean fits(ItemStack output) {
        for (int slot : OUTPUT) {
            ItemStack present = items.get(slot);
            if (present.isEmpty() || ItemStack.isSameItemSameComponents(present, output) && present.getCount() < present.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private void insert(ItemStack output) {
        for (int slot : OUTPUT) {
            ItemStack present = items.get(slot);
            if (ItemStack.isSameItemSameComponents(present, output) && present.getCount() < present.getMaxStackSize()) {
                present.grow(1);
                return;
            }
        }
        for (int slot : OUTPUT) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, output);
                return;
            }
        }
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    /**
     * Output slots are plain slots (2026-09-28): players may put anything in and take anything out. Input
     * slots take only what the station can crush (checked on the server - the client has no recipe table).
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!isInput(slot)) {
            return true;
        }
        return !(level instanceof ServerLevel server) || CrushingRecipes.of(server, stack.getItem()).isPresent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT : INPUT;
    }

    /** Hoppers and droppers still fill only the input - the free output slots are for players. */
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
        return new ThreeInThreeOutMenu(TradeReworkMenus.CRUSHING_STATION_MENU.get(), containerId, inventory, this);
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
