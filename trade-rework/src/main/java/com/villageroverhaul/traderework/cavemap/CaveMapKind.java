package com.villageroverhaul.traderework.cavemap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;

/**
 * The two kinds of cave maps the Cartographer sells (2026-10-10): the Cave Map works underground in the Overworld, the Nether Map in the
 * Nether. A map is bound to the layer it was made in and only draws while its player stands in the layer's drawing band, so it never changes
 * in one stroke. Tweak-Werte.md lists the limits.
 *
 * Making a map (Y, lower limit inclusive):
 * - Overworld: bottom -64 (bedrock) to 0 (where deepslate ends), top 0 to the surface - the Deep Cave Map and the Shallow Cave Map
 * - Nether: bottom 0 to 32 (the lava sea) = the Bottom Nether Cave Map, middle 32 to 70 = the Bottom Nether Map, top 70 to 105 = the Top Nether Map,
 *   everything above 105 up to the roof (128) = the Top Nether Cave Map
 *
 * Drawing: the same band, except that the Bottom and the Top Nether Map both draw from the lava sea (its surface block, Y 31) up to Y 105 - nothing above
 * counts for them. Where two floors compete for one pixel the Top Nether Map lets the higher one win, every other map the lower one (upperWins).
 * Rule for the rays: everything with "Cave" in its name (Cave Maps, the Nether Cave Maps) sees along rays, the Bottom and Top Nether Map test every column.
 */
public enum CaveMapKind {

    CAVE(Level.OVERWORLD, new CaveLayer[]{CaveLayer.BOTTOM, CaveLayer.TOP}, new int[]{-64, 0, 320}),
    NETHER(Level.NETHER, new CaveLayer[]{CaveLayer.BOTTOM, CaveLayer.MIDDLE, CaveLayer.TOP, CaveLayer.CEILING}, new int[]{0, 32, 70, 106, 128});

    private final ResourceKey<Level> dimension;
    private final CaveLayer[] layers;
    private final int[] limits;

    CaveMapKind(ResourceKey<Level> dimension, CaveLayer[] layers, int[] limits) {
        this.dimension = dimension;
        this.layers = layers;
        this.limits = limits;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    private int indexOf(CaveLayer layer) {
        for (int i = 0; i < layers.length; i++) {
            if (layers[i] == layer) {
                return i;
            }
        }
        throw new IllegalArgumentException(layer + " is no layer of " + this);
    }

    /** The layer a map made at this height gets - none above the top limit or below the bottom one. */
    public Optional<CaveLayer> layerAt(int y) {
        for (int i = 0; i < layers.length; i++) {
            if (y >= limits[i] && y < limits[i + 1]) {
                return Optional.of(layers[i]);
            }
        }
        return Optional.empty();
    }

    /** Lower end of the band the layer's maps are made in. */
    public int minY(CaveLayer layer) {
        return limits[indexOf(layer)];
    }

    /** Exclusive. */
    public int maxY(CaveLayer layer) {
        return limits[indexOf(layer) + 1];
    }

    private boolean drawsFromLava(CaveLayer layer) {
        return this == NETHER && (layer == CaveLayer.MIDDLE || layer == CaveLayer.TOP);
    }

    /** Lower end of the band a map of this layer draws in: the Bottom and Top Nether Map reach down to the lava sea's surface block (one below its level). */
    public int drawMinY(CaveLayer layer) {
        return drawsFromLava(layer) ? limits[1] - 1 : minY(layer);
    }

    /** Exclusive upper end of the band a map of this layer draws in. */
    public int drawMaxY(CaveLayer layer) {
        return drawsFromLava(layer) ? limits[3] : maxY(layer);
    }

    /** The player stands where this map draws. */
    public boolean drawsAt(CaveLayer layer, int y) {
        return y >= drawMinY(layer) && y < drawMaxY(layer);
    }

    /** The height the layer's brightness gradient ends at (the Overworld's upper layer counts only up to sea level, Y 64). */
    public int shadeTopY(CaveLayer layer) {
        return layer == CaveLayer.TOP && this == CAVE ? 64 : drawMaxY(layer);
    }

    /** Where two floors of one pixel compete, the higher one wins (the Top Nether Map); otherwise the lower one (all other maps). */
    public boolean upperWins(CaveLayer layer) {
        return this == NETHER && layer == CaveLayer.TOP;
    }

    /** Everything "Cave" (the Cave Maps and both Nether Cave Maps) sees along rays that stop at walls; the Bottom and the Top Nether Map test every column in their radius. */
    public boolean usesRays(CaveLayer layer) {
        return this == CAVE || layer == CaveLayer.BOTTOM || layer == CaveLayer.CEILING;
    }

    /**
     * Where the map can be made: its own dimension, inside a layer and - Cave Map - with no open sky above the head (the column's top block is above it - a
     * surface is no cave; the height map is used, not the sky light, because that is updated later). The Nether has no sky, so only its layers count.
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
