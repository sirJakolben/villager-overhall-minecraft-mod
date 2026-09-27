package com.villageroverhaul.mason;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Flat path stones (Mason, 2026-09-26): up to 4 stones on one block, turned to the player - the same
 * stacking as the rock pile (RockPileBlock), one stone of rock_path.bbmodel (assets/Mason/path_rocks) per
 * step (models/block/rock_path_1..4.json). The model reaches 1 px below the block so the stones sit on a
 * dirt path (15/16 high); they need stable ground below (canSurvive). A dirt path stays a path: the stones
 * aren't solid. No collision (MasonBlocks: noCollision) - players and mobs walk through; the outline below is
 * only for aiming at it, kept inside the block, 1 px high. Breaks instantly by hand.
 */
public class RockPathBlock extends RockPileBlock {

    public static final MapCodec<RockPathBlock> CODEC = simpleCodec(RockPathBlock::new);

    /** Each stone's box for a north-facing block, in 1/16 block. */
    private static final VoxelShape[] STONE_BOXES = {
            Block.box(3, 0, 2, 8, 1, 7),
            Block.box(1, 0, 8, 5, 1, 12),
            Block.box(11, 0, 1, 13, 1, 3),
            Block.box(6, 0, 7, 15, 1, 15)
    };
    @SuppressWarnings("unchecked")
    private static final Map<Direction, VoxelShape>[] SHAPES = new Map[MAX_ROCKS];

    static {
        VoxelShape path = Shapes.empty();
        for (int stones = 1; stones <= MAX_ROCKS; stones++) {
            path = Shapes.or(path, STONE_BOXES[stones - 1]);
            SHAPES[stones - 1] = Shapes.rotateHorizontal(path);
        }
    }

    public RockPathBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<RockPathBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(ROCKS) - 1].get(state.getValue(FACING));
    }

    /**
     * Only on stable ground (2026-09-26): a block whose top can hold something in its center, like a
     * candle needs - so not on another rock path, a carpet or air. A dirt path counts too, though its top
     * sits 1 px lower; that is what the model's 1 px drop is for.
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).is(Blocks.DIRT_PATH) || Block.canSupportCenter(level, below, Direction.UP);
    }
}
