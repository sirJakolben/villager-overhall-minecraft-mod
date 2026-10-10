package com.villageroverhaul.traderework.cavemap;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws a cave map around the player (2026-10-10, idea of the line of sight and the slope shading from MehradN's Cave Survey). From the
 * head, RAYS horizontal rays run to the first wall, at three heights (eye, +2, +4 blocks - so low obstacles and sloping floors do not hide
 * a cave); a ray steps through everything without a collision shape (air, water, grass). Their lengths, interpolated between neighbouring
 * rays, make one polygon per height. For every map pixel inside a polygon:
 * - its column is open at that height: the pixel is the first block with a map colour below (the floor), drawn in the colour that is
 *   nearest to its normal map colour made brighter or darker by the floor's height in the layer (heightShaded) - lava and wood keep their colour
 * - its column is solid there: a wall, drawn black - the outline of the cave
 * All other pixels stay as they were; there is no seeing through walls. Scale 0 only (1 block = 1 pixel); the map has to be one the
 * player stands on. Nothing is drawn outside the map's height band (CaveMapKind.layerAt; the surface is inside the upper one) and nothing is loaded - unloaded chunks end a ray.
 *
 * A floor inside the map's own height range replaces a pixel that shows a floor from outside it (the floor-height rule does not apply to that).
 * A block of STRUCTURE_BLOCKS beats every rule below (the floor-height and the height-range rule, and any colour already there) and the pixel is locked once drawn; only the surface black and an already locked pixel are stronger.
 *
 * A floor outside the map's own height range (below its layer) only fills pixels that are still empty or black, it never overwrites a discovered one.
 *
 * Every coloured pixel remembers the Y of the floor block it shows (only in memory per map); a ray at a higher Y never computes it again, only a ray below that floor can, so
 * the map keeps the lowest picture. Above Y 60 a floor with open sky over it is the surface and drawn black (Overworld). Black never paints over a discovered pixel; a colour replaces black at once. Cost per run: about RAYS * 3 * 2 * RADIUS block lookups plus the pixels inside the
 * polygons (about 3000 columns, a few blocks of scan each) - and only for the held map every DRAW_INTERVAL_TICKS.
 */
public final class CaveMapDrawer {

    public static final int RADIUS = 32;
    public static final int RAYS = 32;
    /** Every pixel within this distance of the player that no ray has shown yet turns black - the unknown close by (Tweak-Werte.md). */
    public static final int BLACKOUT_RADIUS = RADIUS;
    public static final int[] RAY_HEIGHTS = {0, 2, 4};
    public static final int DRAW_INTERVAL_TICKS = 10;
    /** Floors above this height with open sky over them are drawn black - the surface is no cave (Overworld only, Tweak-Werte.md). */
    public static final int SURFACE_BLACK_ABOVE = 60;
    /** The lowest block of a layer is this much darker than the normal colour, the highest this much brighter, linear in between (Tweak-Werte.md) - so the height can be read off the map. */
    public static final double HEIGHT_SHADE = 0.4;
    /** Below the layer: HEIGHT_SHADE plus this much more darkness per block (Tweak-Werte.md); a floor that dark is black. */
    public static final double DARKEN_PER_BLOCK = 0.05;
    /** A discovered floor is never darker than this share of its normal brightness (90 % darker at most, Tweak-Werte.md) - it stays a real, if almost black, pixel. */
    private static final double MIN_BRIGHTNESS = 0.1;
    /** The ray moves this far per step - half a block never skips a wall. */
    private static final double STEP = 0.5;
    private static final byte WALL_COLOR = MapColor.COLOR_BLACK.getPackedId(MapColor.Brightness.LOWEST);
    /** Per map pixel: the Y of the floor block it shows (NO_FLOOR = not coloured yet). A ray at or above that height never redraws the pixel. Only in memory; a restart starts over. */
    private static final Map<MapId, int[]> PIXEL_FLOORS = new ConcurrentHashMap<>();
    /** Block tag of the blocks structures are built from - mineshaft wood, dungeon cobblestone, trial chamber tuff and copper ... (data/vo_trade_rework/tags/block/cave_map_structure_blocks.json; extend it for other mods). */
    public static final TagKey<Block> STRUCTURE_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "cave_map_structure_blocks"));
    /** Per map: pixels that show a structure block - once drawn they are never changed again (only in memory). */
    private static final Map<MapId, java.util.BitSet> STRUCTURE_PIXELS = new ConcurrentHashMap<>();
    private static final int NO_FLOOR = Integer.MAX_VALUE;

    private CaveMapDrawer() {
    }

    /** True when this player's held map should be drawn this tick (spread over the players by id). */
    public static boolean isDue(ServerLevel level, Player player) {
        return (level.getGameTime() + player.getId()) % DRAW_INTERVAL_TICKS == 0;
    }

    public static void forget(MapId id) {
        PIXEL_FLOORS.remove(id);
        STRUCTURE_PIXELS.remove(id);
    }

    /** Draws what the player sees into the map - a no-op when the player is not in the map's layer or the map's dimension. */
    public static void draw(ServerLevel level, Player player, MapId id, MapItemSavedData data, CaveMapKind kind, CaveLayer layer) {
        BlockPos head = BlockPos.containing(player.getEyePosition());
        // An existing map is drawn wherever the player is in its height band - also on the surface, where everything turns black; only making one needs a cave (CaveMapKind.usableLayer).
        if (data.dimension != level.dimension() || kind.layerAt(head.getY()).filter(layer::equals).isEmpty()) {
            return;
        }
        double originX = player.getX();
        double originZ = player.getZ();
        int mapMinX = data.centerX - 64;
        int mapMinZ = data.centerZ - 64;
        java.util.BitSet structurePixels = STRUCTURE_PIXELS.computeIfAbsent(id, key -> new java.util.BitSet(128 * 128));
        int[] floors = PIXEL_FLOORS.computeIfAbsent(id, key -> {
            int[] values = new int[128 * 128];
            java.util.Arrays.fill(values, NO_FLOOR);
            return values;
        });
        int layerMin = kind.minY(layer);
        int layerTop = kind.shadeTopY(layer);
        int layerMax = kind.maxY(layer);
        int floorLimit = level.getMinY(); // the floor search goes below the layer too (darker the deeper), see heightShaded
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int heights = RAY_HEIGHTS.length;
        // The upper heights only matter where the lower ones found no open column, so their rays are cast on first need (lazily).
        double[][] reach = new double[heights][];
        int[] startY = new int[heights];
        boolean[] reachable = new boolean[heights];
        for (int h = 0; h < heights; h++) {
            startY[h] = Math.min(head.getY() + RAY_HEIGHTS[h], Math.min(kind.maxY(layer), level.getMaxY()) - 1);
            // A raised ray only counts if nothing solid hangs between the head and its height - otherwise it would run through another cave above the ceiling.
            reachable[h] = true;
            for (int y = head.getY(); y <= startY[h] && reachable[h]; y++) {
                reachable[h] = passable(level, pos.set(head.getX(), y, head.getZ()));
            }
        }

        int minPixelX = Math.max(0, Mth.floor(originX - RADIUS) - mapMinX);
        int maxPixelX = Math.min(127, Mth.floor(originX + RADIUS) - mapMinX);
        int minPixelZ = Math.max(0, Mth.floor(originZ - RADIUS) - mapMinZ);
        int maxPixelZ = Math.min(127, Mth.floor(originZ + RADIUS) - mapMinZ);
        for (int pixelX = minPixelX; pixelX <= maxPixelX; pixelX++) {
            for (int pixelZ = minPixelZ; pixelZ <= maxPixelZ; pixelZ++) {
                int blockX = mapMinX + pixelX;
                int blockZ = mapMinZ + pixelZ;
                double dx = blockX + 0.5 - originX;
                double dz = blockZ + 0.5 - originZ;
                double distance = Math.sqrt(dx * dx + dz * dz);
                int index = pixelX + pixelZ * 128;
                if (structurePixels.get(index)) {
                    continue; // a structure block is shown here - it is kept as it is
                }
                boolean seen = false;
                boolean open = false;
                int floorY = Integer.MIN_VALUE;
                MapColor color = MapColor.NONE;
                boolean exempt = false;
                boolean structure = false;
                for (int h = 0; h < heights && color == MapColor.NONE; h++) {
                    if (!reachable[h]) {
                        continue;
                    }
                    if (reach[h] == null) {
                        reach[h] = castRays(level, pos, originX, originZ, startY[h]);
                    }
                    if (distance > reachAt(reach[h], dx, dz)) {
                        continue;
                    }
                    seen = true;
                    if (!passable(level, pos.set(blockX, startY[h], blockZ))) {
                        continue;
                    }
                    open = true;
                    for (int y = startY[h]; y >= floorLimit; y--) {
                        BlockState state = level.getBlockState(pos.set(blockX, y, blockZ));
                        MapColor stateColor = state.getMapColor(level, pos);
                        if (stateColor != MapColor.NONE) {
                            color = stateColor;
                            floorY = y;
                            exempt = keepsItsColour(state);
                            structure = state.is(STRUCTURE_BLOCKS);
                            boolean fixesWrongLayer = floorY >= layerMin && floorY < layerMax && floors[index] != NO_FLOOR && (floors[index] < layerMin || floors[index] >= layerMax);
                            if (!structure && !fixesWrongLayer && startY[h] > floors[index]) {
                                // The pixel shows a floor below this ray: only a structure block may change it, so look on at the higher rays.
                                color = MapColor.NONE;
                                break;
                            }
                            break;
                        }
                    }
                }
                byte existing = data.colors[index];
                byte packed;
                boolean skyBlack = false;
                if (color != MapColor.NONE) {
                    skyBlack = kind == CaveMapKind.CAVE && floorY > SURFACE_BLACK_ABOVE
                            && isSkyBlack(kind, floorY, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mapMinX + pixelX, mapMinZ + pixelZ));
                    packed = skyBlack ? WALL_COLOR : exempt ? color.getPackedId(MapColor.Brightness.NORMAL) : heightShaded(color, floorY, layerMin, layerTop);
                } else {
                    if (seen && !open) {
                        packed = WALL_COLOR; // inside the view and solid at every height: a wall
                    } else if (!seen && existing == 0 && distance <= BLACKOUT_RADIUS) {
                        packed = WALL_COLOR; // beyond where the rays ended: still unknown near the player turns black
                    } else {
                        continue; // an open column without a floor in this layer, or outside the blackout circle, stays untouched
                    }
                }
                if (color != MapColor.NONE && !structure && (floorY < layerMin || floorY >= layerMax) && existing != 0 && existing != WALL_COLOR) {
                    continue; // a floor outside the map's own height range (a deep cave seen on a shallow map) only fills gaps, it never overwrites
                }
                if (packed == WALL_COLOR) {
                    // Black never paints over a discovered pixel.
                    if (existing != 0) {
                        continue;
                    }
                }
                data.updateColor(pixelX, pixelZ, packed);
                if (color != MapColor.NONE && !skyBlack) {
                    floors[index] = floorY;
                    if (structure) {
                        structurePixels.set(index);
                    }
                }
            }
        }
    }

    private static double[] castRays(ServerLevel level, BlockPos.MutableBlockPos pos, double originX, double originZ, int y) {
        double[] reach = new double[RAYS];
        for (int ray = 0; ray < RAYS; ray++) {
            double angle = ray * (Mth.TWO_PI / RAYS);
            reach[ray] = castRay(level, pos, originX, originZ, y, Math.cos(angle), Math.sin(angle));
        }
        return reach;
    }

    /** Distance in blocks to just behind the first wall (one block more); unloaded chunks count as a wall. Capped at RADIUS when nothing is hit. */
    private static double castRay(ServerLevel level, BlockPos.MutableBlockPos pos, double originX, double originZ, int y, double dirX, double dirZ) {
        double distance = 0.0;
        while (distance < RADIUS) {
            double next = distance + STEP;
            if (!passable(level, pos.set(Mth.floor(originX + dirX * next), y, Mth.floor(originZ + dirZ * next)))) {
                return distance + 1.0; // the wall block itself is inside the view - it is drawn black
            }
            distance = next;
        }
        return RADIUS;
    }

    /** The ray length at this offset's angle, interpolated between the two rays next to it. */
    private static double reachAt(double[] reach, double dx, double dz) {
        double angle = Math.atan2(dz, dx);
        if (angle < 0) {
            angle += Mth.TWO_PI;
        }
        double position = angle / (Mth.TWO_PI / RAYS);
        int low = (int) position % RAYS;
        int high = (low + 1) % RAYS;
        double fraction = position - Math.floor(position);
        return reach[low] + (reach[high] - reach[low]) * fraction;
    }

    /** Free for a ray: not loaded counts as a wall, a block without collision (air, water, plants) is passable. */
    private static boolean passable(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** A floor above SURFACE_BLACK_ABOVE with nothing over it but sky (the column's height map is at most one above it) is the surface - black. Overworld only. */
    public static boolean isSkyBlack(CaveMapKind kind, int floorY, int columnTop) {
        return kind == CaveMapKind.CAVE && floorY > SURFACE_BLACK_ABOVE && floorY + 1 >= columnTop;
    }

    /** Lava (a danger sign) and wood (mine shafts and buildings) are not shaded by height - they keep their normal map colour. */
    private static boolean keepsItsColour(BlockState state) {
        SoundType sound = state.getSoundType();
        return state.getFluidState().is(FluidTags.LAVA) || sound == SoundType.WOOD || sound == SoundType.NETHER_WOOD || sound == SoundType.CHERRY_WOOD
                || sound == SoundType.BAMBOO_WOOD || sound == SoundType.BAMBOO;
    }

    /** Every colour a map can show (base colour x four brightness steps) with its RGB - built once. */
    private static final class Palette {
        static final byte[] PACKED;
        static final int[] RED;
        static final int[] GREEN;
        static final int[] BLUE;

        static {
            java.util.List<Integer> ids = new java.util.ArrayList<>();
            for (int base = 1; base < 64; base++) {
                if (MapColor.byId(base) != MapColor.NONE) {
                    for (MapColor.Brightness brightness : MapColor.Brightness.values()) {
                        ids.add(MapColor.byId(base).getPackedId(brightness) & 0xFF);
                    }
                }
            }
            PACKED = new byte[ids.size()];
            RED = new int[ids.size()];
            GREEN = new int[ids.size()];
            BLUE = new int[ids.size()];
            for (int i = 0; i < PACKED.length; i++) {
                PACKED[i] = (byte) (int) ids.get(i);
                int argb = MapColor.getColorFromPackedId(ids.get(i));
                RED[i] = ARGB.red(argb);
                GREEN[i] = ARGB.green(argb);
                BLUE[i] = ARGB.blue(argb);
            }
        }
    }

    private static final Map<Integer, Byte> SHADED = new ConcurrentHashMap<>();

    /**
     * The floor colour by its height in the map's layer: the base colour in its normal brightness, made HEIGHT_SHADE darker at the layer's lowest block and HEIGHT_SHADE brighter at
     * its top (CaveMapKind.shadeTopY), linear between. Below the layer: HEIGHT_SHADE plus DARKEN_PER_BLOCK for every block further down - a deep cave on a shallow map fades
     * away, down to MIN_BRIGHTNESS. A map pixel can only be one of the Vanilla palette colours, so the result is the palette colour nearest (in RGB) to the shifted colour - no dithering; the darkness stops at 90 %. Results are cached per base colour and shift (in 1 % steps).
     */
    public static byte heightShaded(MapColor color, int floorY, int layerMin, int layerTop) {
        double shift;
        if (floorY >= layerMin) {
            double t = Mth.clamp((floorY - layerMin) / (double) Math.max(1, layerTop - layerMin), 0.0, 1.0);
            shift = -HEIGHT_SHADE + 2.0 * HEIGHT_SHADE * t;
        } else {
            shift = -(HEIGHT_SHADE + (layerMin - floorY) * DARKEN_PER_BLOCK);
        }
        double factor = Math.max(MIN_BRIGHTNESS, 1.0 + shift);
        int step = (int) Math.round(factor * 100.0);
        return SHADED.computeIfAbsent(color.id * 1000 + step, key -> nearest(color, step / 100.0));
    }

    private static byte nearest(MapColor color, double factor) {
        int argb = color.calculateARGBColor(MapColor.Brightness.NORMAL);
        double red = Math.min(255.0, ARGB.red(argb) * factor);
        double green = Math.min(255.0, ARGB.green(argb) * factor);
        double blue = Math.min(255.0, ARGB.blue(argb) * factor);
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < Palette.PACKED.length; i++) {
            double dr = Palette.RED[i] - red;
            double dg = Palette.GREEN[i] - green;
            double db = Palette.BLUE[i] - blue;
            double distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return Palette.PACKED[best];
    }
}
