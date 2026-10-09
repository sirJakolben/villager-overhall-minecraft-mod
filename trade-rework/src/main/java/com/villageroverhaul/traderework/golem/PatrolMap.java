package com.villageroverhaul.traderework.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The patrol map of a dimension (Obsidian Iron Golem, 2026-10-09): one patrol point per chunk, the path block (dirt path first) closest to the chunk's
 * middle. A village is what lies around its bell: the chunks within VILLAGE_CHUNKS of it (a village without a bell: around the golem), so a village a
 * player made counts like a generated one. The golems fill the map bit by bit while they patrol (SCAN_PER_PLAN chunks per plan, loaded ones only) -
 * after every restart anew, nothing is saved - and a path block a player lays in a chunk without a point is added at once (PatrolEvents).
 * A golem always walks to the point that was visited longest ago, so together they cover the whole village.
 *
 * Cost: one chunk scan is about 256 columns x 4 blocks; a plan scans at most SCAN_PER_PLAN of them and then only reads the map.
 */
final class PatrolMap {

    static final int VILLAGE_CHUNKS = 4;
    private static final int SCAN_PER_PLAN = 2;
    /** A point nobody could reach is not tried again for this long (ticks, 5 min). */
    private static final long UNREACHABLE_RETRY_TICKS = 6000L;
    /** The bell is searched this far (blocks). */
    private static final int BELL_REACH = 64;

    private static final Map<ResourceKey<Level>, PatrolMap> MAPS = new HashMap<>();

    private final Map<ChunkPos, Point> points = new HashMap<>();
    private final Set<ChunkPos> scanned = new HashSet<>();
    private final Map<ChunkPos, Long> retryAt = new HashMap<>();
    private final Set<BlockPos> unreachable = new HashSet<>();

    private static final class Point {
        final BlockPos pos;
        final boolean primary;
        long lastVisited;

        Point(BlockPos pos, boolean primary) {
            this.pos = pos;
            this.primary = primary;
        }
    }

    static PatrolMap of(ServerLevel level) {
        return MAPS.computeIfAbsent(level.dimension(), key -> new PatrolMap());
    }

    static void clearAll() {
        MAPS.clear();
    }

    /** The bell of the village around this position - or the position itself if there is none. */
    static BlockPos centerOf(ServerLevel level, BlockPos here) {
        return level.getPoiManager()
                .getInRange(poi -> poi.is(PoiTypes.MEETING), here, BELL_REACH, PoiManager.Occupancy.ANY)
                .map(record -> record.getPos())
                .min(Comparator.comparingDouble(pos -> pos.distSqr(here)))
                .orElse(here);
    }

    private static boolean inVillage(ChunkPos chunk, BlockPos center) {
        return Math.abs(chunk.x() - (center.getX() >> 4)) <= VILLAGE_CHUNKS && Math.abs(chunk.z() - (center.getZ() >> 4)) <= VILLAGE_CHUNKS;
    }

    /** Scans up to SCAN_PER_PLAN of the village's chunks that are loaded and not scanned yet, the ones closest to the center first. */
    void scanSome(ServerLevel level, BlockPos center) {
        long now = level.getGameTime();
        ChunkPos middle = ChunkPos.containing(center);
        List<ChunkPos> open = new ArrayList<>();
        for (int dx = -VILLAGE_CHUNKS; dx <= VILLAGE_CHUNKS; dx++) {
            for (int dz = -VILLAGE_CHUNKS; dz <= VILLAGE_CHUNKS; dz++) {
                ChunkPos chunk = new ChunkPos(middle.x() + dx, middle.z() + dz);
                if (!scanned.contains(chunk) && retryAt.getOrDefault(chunk, 0L) <= now && level.hasChunk(chunk.x(), chunk.z())) {
                    open.add(chunk);
                }
            }
        }
        open.sort(Comparator.comparingInt(chunk -> Math.abs(chunk.x() - middle.x()) + Math.abs(chunk.z() - middle.z())));
        for (int i = 0; i < Math.min(SCAN_PER_PLAN, open.size()); i++) {
            scan(level, open.get(i));
        }
    }

    private void scan(ServerLevel level, ChunkPos chunk) {
        scanned.add(chunk);
        points.remove(chunk);
        int middleX = chunk.getMiddleBlockX();
        int middleZ = chunk.getMiddleBlockZ();
        BlockPos best = null;
        boolean bestPrimary = false;
        double bestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = top - 1; y >= top - 4; y--) {
                    pos.set(x, y, z);
                    if (!PathBlocks.isPath(level, pos, PathBlocks.LIKE) || unreachable.contains(pos)) {
                        continue;
                    }
                    boolean primary = level.getBlockState(pos).is(Blocks.DIRT_PATH);
                    double distance = (x - middleX) * (x - middleX) + (z - middleZ) * (double) (z - middleZ);
                    if (best == null || primary && !bestPrimary || primary == bestPrimary && distance < bestDistance) {
                        best = pos.immutable();
                        bestPrimary = primary;
                        bestDistance = distance;
                    }
                    break;
                }
            }
        }
        if (best != null) {
            points.put(chunk, new Point(best, bestPrimary));
        }
    }

    /**
     * The point of the village that was visited longest ago, 16 to maxDistance blocks from the golem; it is marked as visited now. A point whose
     * block is gone is dropped and its chunk is scanned again.
     */
    Optional<BlockPos> next(ServerLevel level, BlockPos here, BlockPos center, int minDistance, int maxDistance) {
        long now = level.getGameTime();
        List<Point> candidates = new ArrayList<>();
        for (Map.Entry<ChunkPos, Point> entry : points.entrySet()) {
            double distance = entry.getValue().pos.distSqr(here);
            if (inVillage(entry.getKey(), center) && distance >= minDistance * minDistance && distance <= maxDistance * maxDistance) {
                candidates.add(entry.getValue());
            }
        }
        candidates.sort(Comparator.comparingLong((Point point) -> point.lastVisited).thenComparingInt(point -> point.primary ? 0 : 1));
        for (Point point : candidates) {
            if (PathBlocks.isPath(level, point.pos, PathBlocks.LIKE)) {
                point.lastVisited = now;
                return Optional.of(point.pos);
            }
            ChunkPos chunk = ChunkPos.containing(point.pos);
            points.remove(chunk);
            scanned.remove(chunk);
        }
        return Optional.empty();
    }

    /** A golem could not get to this point: it is not used for a while (it can be a roof or an island) and its chunk is scanned again later. */
    void unreachable(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = ChunkPos.containing(pos);
        Point point = points.get(chunk);
        if (point != null && point.pos.equals(pos)) {
            points.remove(chunk);
            unreachable.add(pos);
            scanned.remove(chunk);
            retryAt.put(chunk, level.getGameTime() + UNREACHABLE_RETRY_TICKS);
        }
    }

    /** A path block was placed: it becomes the point of its chunk if the chunk has none (or only a substitute and this is a dirt path). */
    void added(ServerLevel level, BlockPos pos, Block block) {
        if (!PathBlocks.LIKE.contains(block) || unreachable.contains(pos)) {
            return;
        }
        BlockPos center = centerOf(level, pos);
        if (!inVillage(ChunkPos.containing(pos), center) || center.equals(pos) && !level.isVillage(pos)) {
            return;
        }
        ChunkPos chunk = ChunkPos.containing(pos);
        Point existing = points.get(chunk);
        boolean primary = block == Blocks.DIRT_PATH;
        if (existing == null || primary && !existing.primary) {
            points.put(chunk, new Point(pos.immutable(), primary));
        }
    }
}
