package com.villageroverhaul.explore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Searches chunks ring by ring around an origin (square rings, growing by one chunk) and returns the
 * closest accepted hit of the first ring that has one - like Vanilla's structure search, close enough to
 * "the nearest" for a map, and it stops as soon as something is found.
 *
 * accept checks a candidate before it counts (dungeons: is there really a spawner?); candidates of a ring
 * are checked closest first, and after maxChecks rejected candidates the search gives up.
 */
final class ChunkSearch {

    private ChunkSearch() {
    }

    static Optional<BlockPos> nearest(BlockPos origin, int radiusChunks, Function<ChunkPos, Stream<BlockPos>> hitsInChunk) {
        return nearest(origin, radiusChunks, hitsInChunk, pos -> true, Integer.MAX_VALUE);
    }

    static Optional<BlockPos> nearest(BlockPos origin, int radiusChunks, Function<ChunkPos, Stream<BlockPos>> hitsInChunk,
                                      Predicate<BlockPos> accept, int maxChecks) {
        ChunkPos center = ChunkPos.containing(origin);
        Comparator<BlockPos> byDistance = Comparator.comparingDouble(pos -> pos.distSqr(origin));
        int checks = 0;
        for (int ring = 0; ring <= radiusChunks; ring++) {
            List<BlockPos> candidates = new ArrayList<>();
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue; // inner rings are done already
                    }
                    hitsInChunk.apply(new ChunkPos(center.x() + dx, center.z() + dz)).forEach(candidates::add);
                }
            }
            candidates.sort(byDistance);
            for (BlockPos candidate : candidates) {
                if (accept.test(candidate)) {
                    return Optional.of(candidate);
                }
                if (++checks >= maxChecks) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}
