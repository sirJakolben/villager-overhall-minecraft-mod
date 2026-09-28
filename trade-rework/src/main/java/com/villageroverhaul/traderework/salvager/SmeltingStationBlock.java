package com.villageroverhaul.traderework.salvager;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Salvager's passive station (2026-09-27): a 3 + 3 slot container (SmeltingStationBlockEntity) that opens its window
 * on right-click. Like the Librarian's enchantment station, every work step gives a short redstone pulse
 * to all sides and comparators read the fill level. Stays a Salvager job site in all states
 * (ProfessionStations adds all states of the block).
 *
 * WORKING fills the crucible with the animated molten metal while the Salvager works here (set by
 * SmeltingStationBlockEntity.serverTick); the metal glows at full brightness and the station gives WORKING_LIGHT like a lit blast furnace.
 * The metal also pops lava bubbles with their sound (animateTick).
 * An idle station shows the crucible empty and gives no light.
 */
public class SmeltingStationBlock extends BaseEntityBlock {

    public static final MapCodec<SmeltingStationBlock> CODEC = simpleCodec(SmeltingStationBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    /** Block light while WORKING - the same as a lit blast furnace. */
    public static final int WORKING_LIGHT = 13;
    /** One in this many animate ticks pops a lava bubble from the molten metal while WORKING - the campfire's rate. */
    public static final int BUBBLE_CHANCE = 5;
    /** Length of the pulse after a work step, in game ticks - an observer's pulse, same as the enchantment station. */
    public static final int PULSE_TICKS = 2;
    /** Base, hearth and crucible rim (models/block/smelting_station.json) - the model does not fill the block, hence noOcclusion. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 10, 16),
            Block.box(1, 10, 1, 15, 14, 15),
            Block.box(3, 14, 3, 13, 16, 13)
    );

    public SmeltingStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED, false).setValue(WORKING, false));
    }

    @Override
    protected MapCodec<SmeltingStationBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, WORKING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmeltingStationBlockEntity(pos, state);
    }

    /** Server only: the station plays the blast furnace crackle while its Salvager fills the passive meter here. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel
                ? createTickerHelper(type, SalvagerBlockEntities.SMELTING_STATION.get(), (tickLevel, pos, tickState, station) -> station.serverTick((ServerLevel) tickLevel, pos))
                : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof SmeltingStationBlockEntity station) {
            player.openMenu(station);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Client only: while WORKING the molten metal pops lava bubbles like a lava pool (MOLTEN_BUBBLE, a smaller lava particle, and the pop sound), from a random spot on its
     * 6 x 6 px surface (models/block/smelting_station_working.json). Needs no packet - the client knows WORKING.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(WORKING) && random.nextInt(BUBBLE_CHANCE) == 0) {
            double x = pos.getX() + (5 + random.nextDouble() * 6) / 16.0;
            double z = pos.getZ() + (5 + random.nextDouble() * 6) / 16.0;
            double y = pos.getY() + 14.1 / 16.0;
            level.addParticle(SalvagerParticles.MOLTEN_BUBBLE.get(), x, y, z, 0.0, 0.0, 0.0);
            // Volume and pitch as a lava pool's pop (LavaFluid.animateTick).
            level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
        }
    }

    /** Switches the station on for PULSE_TICKS; setBlock notifies the neighbors, the scheduled tick switches it off. */
    public static void pulse(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SmeltingStationBlock block)) {
            return;
        }
        if (!state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, block, PULSE_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }
}
