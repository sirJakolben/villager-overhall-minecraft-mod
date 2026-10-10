package com.villageroverhaul.traderework.cavemap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

/**
 * The two kinds of cave maps the Cartographer sells (2026-10-10): the Cave Map works underground in the Overworld, the Nether Map in the
 * Nether. Each splits its dimension into two height layers (Layer); a map is bound to the layer it was made in and only draws while
 * its player stands in that layer, so it never changes in one stroke. Tweak-Werte.md lists the limits.
 *
 * Layer limits (Y, lower inclusive): Overworld -64 (bedrock) / 0 (where deepslate ends) / up to the surface, Nether 0 / 32 (the lava sea) / 128 (the roof).
 */
public enum CaveMapKind {

    CAVE(Level.OVERWORLD, new int[]{-64, 0, 320}),
    NETHER(Level.NETHER, new int[]{0, 32, 128});

    private final ResourceKey<Level> dimension;
    private final int[] limits;

    CaveMapKind(ResourceKey<Level> dimension, int[] limits) {
        this.dimension = dimension;
        this.limits = limits;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    /** The layer this height belongs to - none above the top limit or below the bottom one. */
    public Optional<CaveLayer> layerAt(int y) {
        for (CaveLayer layer : CaveLayer.values()) {
            if (y >= minY(layer) && y < maxY(layer)) {
                return Optional.of(layer);
            }
        }
        return Optional.empty();
    }

    /** The height the layer's brightness gradient ends at (the layer's top; the Overworld's upper layer counts only up to sea level, Y 64). */
    public int shadeTopY(CaveLayer layer) {
        return layer == CaveLayer.TOP && this == CAVE ? 64 : maxY(layer);
    }

    public int minY(CaveLayer layer) {
        return limits[layer.ordinal()];
    }

    /** Exclusive. */
    public int maxY(CaveLayer layer) {
        return limits[layer.ordinal() + 1];
    }

    /**
     * Where the map can be made and drawn: its own dimension, inside a layer and - Cave Map - with no open sky above the head (the column's top block is above it - a surface is no cave; the height map is used, not the sky light, because that is
     * updated later). The Nether has no sky, so only its layers count.
     */
    public Optional<CaveLayer> usableLayer(Level level, BlockPos head) {
        if (level.dimension() != dimension) {
            return Optional.empty();
        }
        if (this == CAVE && head.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, head.getX(), head.getZ())) {
            return Optional.empty();
        }
        return layerAt(head.getY());
    }
}
