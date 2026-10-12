package com.smartvillager.professions.goldsmith;

import com.mojang.serialization.MapCodec;
import com.smartvillager.professions.station.PassiveStationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Goldsmith's passive station (2026-09-27): a 3 + 3 slot container (SalvageStationBlockEntity) that opens its window
 * on right-click. Pulse, comparator and pathing come from PassiveStationBlock. Stays a Goldsmith job site in all
 * states (ProfessionStations adds all states of the block).
 *
 * WORKING fills the crucible with the animated molten metal while the Goldsmith works here (set by
 * SalvageStationBlockEntity.serverTick); the metal glows at full brightness and the station gives WORKING_LIGHT like a lit blast furnace.
 * The metal also pops lava bubbles with their sound (animateTick).
 * An idle station shows the crucible empty and gives no light.
 */
public class SalvageStationBlock extends PassiveStationBlock {

    public static final MapCodec<SalvageStationBlock> CODEC = simpleCodec(SalvageStationBlock::new);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    /** Block light while WORKING - the same as a lit blast furnace. */
    public static final int WORKING_LIGHT = 13;
    /** One in this many animate ticks pops a lava bubble from the molten metal while WORKING - the campfire's rate. */
    public static final int BUBBLE_CHANCE = 5;
    /** Base, hearth and crucible rim (models/block/salvage_station.json) - the model does not fill the block, hence noOcclusion. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 10, 16),
            Block.box(1, 10, 1, 15, 14, 15),
            Block.box(3, 14, 3, 13, 16, 13)
    );

    public SalvageStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WORKING, false));
    }

    @Override
    protected MapCodec<SalvageStationBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORKING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SalvageStationBlockEntity(pos, state);
    }

    /** Server only: the station plays the blast furnace crackle while its Goldsmith fills the passive meter here. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel
                ? createTickerHelper(type, GoldsmithBlockEntities.SALVAGE_STATION.get(), (tickLevel, pos, tickState, station) -> station.serverTick((ServerLevel) tickLevel, pos))
                : null;
    }

    /**
     * Client only: while WORKING the molten metal pops lava bubbles like a lava pool (MOLTEN_BUBBLE, a smaller lava particle, and the pop sound), from a random spot on its
     * 6 x 6 px surface (models/block/salvage_station_working.json). Needs no packet - the client knows WORKING.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(WORKING) && random.nextInt(BUBBLE_CHANCE) == 0) {
            double x = pos.getX() + (5 + random.nextDouble() * 6) / 16.0;
            double z = pos.getZ() + (5 + random.nextDouble() * 6) / 16.0;
            double y = pos.getY() + 14.1 / 16.0;
            level.addParticle(GoldsmithParticles.MOLTEN_BUBBLE.get(), x, y, z, 0.0, 0.0, 0.0);
            // Volume and pitch as a lava pool's pop (LavaFluid.animateTick).
            level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
        }
    }
}
