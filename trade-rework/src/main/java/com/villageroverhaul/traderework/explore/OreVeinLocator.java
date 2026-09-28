package com.villageroverhaul.traderework.explore;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseRouter;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds the big ore veins added in 1.18 (Salvager's vein maps, 2026-09-27) from the world seed alone. They
 * are neither a structure nor a feature but part of the terrain noise (Vanilla OreVeinifier): the vein
 * toggle noise picks copper (positive, y 0..50) or iron (negative, y -60..-8), and where it is strong
 * enough and the ridged noise is below zero, the stone becomes vein.
 *
 * This samples the same noise functions of the level's RandomState at the middle of each chunk every
 * SAMPLE_STEP_Y blocks - no chunk is loaded - and takes the first spot that is inside solid terrain and
 * passes OreVeinifier's own tests, including the ore-gap noise (so the mark sits where ore is, not only
 * granite or tuff). Only the per-block random thinning of the vein is left out.
 */
public final class OreVeinLocator {

    /** OreVeinifier's constants. */
    private static final double VEININESS_THRESHOLD = 0.4;
    private static final double EDGE_ROUNDOFF_BEGIN = 20.0;
    private static final double MAX_EDGE_ROUNDOFF = 0.2;
    private static final double SKIP_ORE_IF_GAP_BELOW = -0.3;
    private static final int SAMPLE_STEP_Y = 4;

    private OreVeinLocator() {
    }

    public static Optional<BlockPos> findNearest(ServerLevel level, VeinType type, BlockPos origin, int radiusChunks) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (!(generator instanceof NoiseBasedChunkGenerator noise) || !noise.generatorSettings().value().oreVeinsEnabled()) {
            return Optional.empty(); // no ore veins in this dimension
        }
        NoiseRouter router = level.getChunkSource().randomState().router();
        return ChunkSearch.nearest(origin, radiusChunks, chunk -> veinIn(router, type, chunk).stream());
    }

    private static Optional<BlockPos> veinIn(NoiseRouter router, VeinType type, ChunkPos chunk) {
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        for (int y = type.minY(); y <= type.maxY(); y += SAMPLE_STEP_Y) {
            DensityFunction.FunctionContext point = new DensityFunction.SinglePointContext(x, y, z);
            double toggle = router.veinToggle().compute(point);
            if (toggle > 0.0 != type.positiveToggle()) {
                continue;
            }
            int distanceFromEdge = Math.min(type.maxY() - y, y - type.minY());
            double edgeRoundoff = Mth.clampedMap(distanceFromEdge, 0.0, EDGE_ROUNDOFF_BEGIN, -MAX_EDGE_ROUNDOFF, 0.0);
            if (Math.abs(toggle) + edgeRoundoff < VEININESS_THRESHOLD
                    || router.veinRidged().compute(point) >= 0.0
                    || router.veinGap().compute(point) <= SKIP_ORE_IF_GAP_BELOW
                    || router.finalDensity().compute(point) <= 0.0) {
                continue;
            }
            return Optional.of(new BlockPos(x, y, z));
        }
        return Optional.empty();
    }
}
