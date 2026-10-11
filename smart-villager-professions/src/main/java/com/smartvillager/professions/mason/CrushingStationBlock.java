package com.smartvillager.professions.mason;

import com.mojang.serialization.MapCodec;
import com.smartvillager.professions.station.FacingPassiveStationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
 * The Mason's passive station: a 3 + 3 slot container (CrushingStationBlockEntity) that opens its window
 * on right-click. Pulse, comparator and pathing come from PassiveStationBlock, placement from
 * FacingPassiveStationBlock: FACING points at the player, so the exhaust (the model's west face while
 * facing east) is at the back. Stays a Mason job site in all states (ProfessionStations adds all states of the
 * block). WORKING switches the blades model to the animated texture while the Mason works here (set by
 * CrushingStationBlockEntity.serverTick).
 */
public class CrushingStationBlock extends FacingPassiveStationBlock {

    public static final MapCodec<CrushingStationBlock> CODEC = simpleCodec(CrushingStationBlock::new);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    /** Two 15 px side walls, the 12 px housing between them (models/block/crushing_station.json, facing east) - the blades sit on the housing. */
    private static final VoxelShape SHAPE_X = Shapes.or(
            Block.box(0, 0, 0, 16, 15, 2),
            Block.box(0, 0, 2, 16, 12, 14),
            Block.box(0, 0, 14, 16, 15, 16)
    );
    /** The same turned by 90 degrees, for facing north or south. */
    private static final VoxelShape SHAPE_Z = Shapes.or(
            Block.box(0, 0, 0, 2, 15, 16),
            Block.box(2, 0, 0, 14, 12, 16),
            Block.box(14, 0, 0, 16, 15, 16)
    );

    public CrushingStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WORKING, false));
    }

    @Override
    protected MapCodec<CrushingStationBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WORKING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrushingStationBlockEntity(pos, state);
    }

    /** Server only: the station knocks out the stone sound while its Mason fills the passive meter here. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel
                ? createTickerHelper(type, MasonBlockEntities.CRUSHING_STATION.get(), (tickLevel, pos, tickState, station) -> station.serverTick((ServerLevel) tickLevel, pos))
                : null;
    }
}
