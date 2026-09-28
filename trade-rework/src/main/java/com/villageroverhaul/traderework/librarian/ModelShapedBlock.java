package com.villageroverhaul.traderework.librarian;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * A plain block whose outline and collision follow its (non-cube) Blockbench model instead of a full
 * cube. Placed like the stonecutter: FACING points at the player, and the shape (given for facing north)
 * turns with it. The shape is passed in per block, so the codec rebuilds the block with that same shape.
 */
public class ModelShapedBlock extends HorizontalDirectionalBlock {

    private final Map<Direction, VoxelShape> shapes;
    private final MapCodec<ModelShapedBlock> codec;

    public ModelShapedBlock(Properties properties, VoxelShape northShape) {
        super(properties);
        this.shapes = Shapes.rotateHorizontal(northShape);
        this.codec = simpleCodec(p -> new ModelShapedBlock(p, northShape));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<ModelShapedBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    /** Not walkable, like Vanilla's lectern - otherwise villagers path through the non-full shape and get stuck. */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
