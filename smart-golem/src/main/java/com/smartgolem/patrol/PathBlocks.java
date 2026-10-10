package com.smartgolem.patrol;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

/** The blocks a village walks on: dirt path first, and the blocks villages often use as a path instead (Iron Golem patrol, Obsidian Iron Golem). */
public final class PathBlocks {

    public static final Set<Block> PRIMARY = Set.of(Blocks.DIRT_PATH);
    public static final Set<Block> LIKE = Set.of(Blocks.DIRT_PATH, Blocks.GRAVEL, Blocks.COBBLESTONE, Blocks.MUD, Blocks.PACKED_MUD, Blocks.MUD_BRICKS,
            Blocks.COARSE_DIRT);

    private PathBlocks() {
    }

    /** A path block of the set with room to walk on it (not under water, nothing solid on top); never loads a chunk. */
    public static boolean isPath(ServerLevel level, BlockPos pos, Set<Block> blocks) {
        if (!level.hasChunkAt(pos) || !blocks.contains(level.getBlockState(pos).getBlock())) {
            return false;
        }
        BlockPos above = pos.above();
        return level.getFluidState(above).isEmpty() && level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }
}
