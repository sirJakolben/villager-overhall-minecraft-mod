package com.villageroverhaul.traderework.librarian;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plain block whose outline and collision follow its (non-cube) Blockbench model instead of a full
 * cube. The shape is passed in per block, so the codec rebuilds the block with that same shape.
 */
public class ModelShapedBlock extends Block {

    private final VoxelShape shape;
    private final MapCodec<ModelShapedBlock> codec;

    public ModelShapedBlock(Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
        this.codec = simpleCodec(p -> new ModelShapedBlock(p, shape));
    }

    @Override
    protected MapCodec<ModelShapedBlock> codec() {
        return codec;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
