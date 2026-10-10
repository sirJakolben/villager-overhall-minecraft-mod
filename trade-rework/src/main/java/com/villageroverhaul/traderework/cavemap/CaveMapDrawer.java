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
 * head, RAYS horizontal rays run to the first wall, at up to three heights (eye, +2 blocks, and +2 above the block looked at - so low obstacles and sloping floors do not hide
 * a cave); a ray steps through everything without a collision shape (air, water, grass). Their lengths, interpolated between neighbouring
 * rays, make one polygon per height. For every map pixel inside a polygon:
 * - its column is open at that height: the pixel is the first block with a map colour below (the floor), drawn in the colour that is
 *   nearest to its normal map colour made brighter or darker by the floor's height in the layer (heightShaded) - lava and wood keep their colour
 * - its column is solid there: a wall, drawn black - the outline of the cave
 * All other pixels stay as they were; there is no seeing through walls. Scale 0 only (1 block = 1 pixel); the map has to be one the
 * player stands on. Nothing is drawn outside the map's height band (CaveMapKind.drawsAt; the surface is inside the upper one) and nothing is loaded - unloaded chunks end a ray.
 *
 * A floor inside the map's own height range replaces a pixel that shows a floor from outside it (the floor-height rule does not apply to that).
 * A block of STRUCTURE_BLOCKS beats every rule below (the floor-height and the height-range rule, and any colour already there) and the pixel is locked once drawn; only the surface black and an already locked pixel are stronger.
 *
 * A floor outside the map's own height range (below its layer) only fills pixels that are still empty or black, it never overwrites a discovered one.
 *
 * Every coloured pixel remembers the Y of the floor block it shows (saved with the world, CaveMapMemory); a ray at a higher Y never computes it again, only a ray below that floor can, so
 * the map keeps the lowest picture. Above Y 60 a floor with open sky over it is the surface and drawn black (Overworld). Black never paints over a discovered pixel; a colour replaces black at once. Cost per run: about RAYS * 3 * 2 * RADIUS block lookups plus the pixels inside the
 * polygons (about 3000 columns, a few blocks of scan each) - and only for the held map every DRAW_INTERVAL_TICKS.
 */
public final class CaveMapDrawer {

    public static final int RADIUS = 51;
    public static final int RAYS = 32;
    /** Every pixel within this distance of the player that no ray has shown yet turns black - the unknown close by (Tweak-Werte.md). */
    public static final int BLACKOUT_RADIUS = RADIUS;
    /** The second ray runs this far above the eye, the third this far above the block the player looks at (Tweak-Werte.md). */
    public static final int RAY_OFFSET = 2;
    public static final int LOOK_OFFSET = 2;
    public static final int DRAW_INTERVAL_TICKS = 10;
    /** Floors above this height with open sky over them are drawn black - the surface is no cave (Overworld only, Tweak-Werte.md). */
    public static final int SURFACE_BLACK_ABOVE = 60;
    /** The lowest block of a layer is this much darker than the normal colour, linear up to HEIGHT_BRIGHTEN brighter at the highest (Tweak-Werte.md) - so the height can be read off the map. */
    public static final double HEIGHT_SHADE = 0.4;
    /** The highest block of a layer is this much brighter than the normal colour (Tweak-Werte.md). */
    public static final double HEIGHT_BRIGHTEN = 0.4;
    /** The Nether's gradient is shifted 10 points up (Tweak-Werte.md): 30 % darker at the bottom of a layer, 50 % brighter at its top. */
    public static final double NETHER_SHADE = 0.3;
    public static final double NETHER_BRIGHTEN = 0.5;
    /** Below the layer: HEIGHT_SHADE plus this much more darkness per block (Tweak-Werte.md); a floor that dark is black. */
    public static final double DARKEN_PER_BLOCK = 0.05;
    /** A discovered floor is never darker than this share of its normal brightness (90 % darker at most, Tweak-Werte.md) - it stays a real, if almost black, pixel. */
    private static final double MIN_BRIGHTNESS = 0.1;
    /** The ray moves this far per step - half a block never skips a wall. */
    private static final double STEP = 0.5;
    /** What walls and the unknown close by look like: black in the Overworld, netherrack-coloured in the Nether (Tweak-Werte.md). Never overwrites a discovered pixel, a colour replaces it. */
    public static byte wallColor(CaveMapKind kind) {
        return (kind == CaveMapKind.NETHER ? MapColor.NETHER : MapColor.COLOR_BLACK).getPackedId(MapColor.Brightness.LOWEST);
    }
    /** Block tag of the blocks structures are built from - mineshaft wood, dungeon cobblestone, trial chamber tuff and copper ... (data/vo_trade_rework/tags/block/cave_map_structure_blocks.json; extend it for other mods). */
    public static final TagKey<Block> STRUCTURE_BLOCKS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "cave_map_structure_blocks"));

    private CaveMapDrawer() {
    }

    /** True when this player's held map should be drawn this tick (spread over the players by id). */
    public static boolean isDue(ServerLevel level, Player player) {
        return (level.getGameTime() + player.getId()) % DRAW_INTERVAL_TICKS == 0;
    }

    /**
     * The map was zoomed out at the cartography table: the picture and the pixel memory move to the new map, whose pixels are twice as coarse. Four old pixels become
     * one; a colour wins over black, a structure pixel over everything, otherwise the first one stays.
     */
    public static void migrate(ServerLevel level, CaveMapKind kind, MapId oldId, MapItemSavedData old, MapId newId, MapItemSavedData zoomed) {
        byte wall = wallColor(kind);
        int oldStep = 1 << old.scale;
        int newStep = 1 << zoomed.scale;
        int oldMinX = old.centerX - 64 * oldStep;
        int oldMinZ = old.centerZ - 64 * oldStep;
        int newMinX = zoomed.centerX - 64 * newStep;
        int newMinZ = zoomed.centerZ - 64 * newStep;
        CaveMapMemory oldMemory = CaveMapMemory.of(level, oldId);
        CaveMapMemory newMemory = new CaveMapMemory();
        int[] oldFloors = oldMemory.floors;
        java.util.BitSet oldStructure = oldMemory.structure;
        int[] newFloors = newMemory.floors;
        java.util.BitSet newStructure = newMemory.structure;
        for (int pixelZ = 0; pixelZ < 128; pixelZ++) {
            for (int pixelX = 0; pixelX < 128; pixelX++) {
                int oldIndex = pixelX + pixelZ * 128;
                byte color = old.colors[oldIndex];
                if (color == 0) {
                    continue;
                }
                int newX = Math.floorDiv(oldMinX + pixelX * oldStep - newMinX, newStep);
                int newZ = Math.floorDiv(oldMinZ + pixelZ * oldStep - newMinZ, newStep);
                if (newX < 0 || newX > 127 || newZ < 0 || newZ > 127) {
                    continue;
                }
                int newIndex = newX + newZ * 128;
                byte existing = zoomed.colors[newIndex];
                boolean structure = oldStructure.get(oldIndex);
                if (existing == 0 || existing == wall && color != wall || structure && !newStructure.get(newIndex)) {
                    zoomed.colors[newIndex] = color;
                    newFloors[newIndex] = oldFloors[oldIndex];
                    newStructure.set(newIndex, structure);
                }
            }
        }
        zoomed.setDirty();
        CaveMapMemory.put(level, newId, newMemory);
    }

    /** Draws what the player sees into the map - a no-op when the player is not in the map's layer or the map's dimension. */
    public static void draw(ServerLevel level, Player player, MapId id, MapItemSavedData data, CaveMapKind kind, CaveLayer layer) {
        BlockPos head = BlockPos.containing(player.getEyePosition());
        // An existing map is drawn wherever the player is in its height band - also on the surface, where everything turns black; only making one needs a cave (CaveMapKind.usableLayer).
        if (data.dimension != level.dimension() || !kind.drawsAt(layer, head.getY())) {
            return;
        }
        double originX = player.getX();
        double originZ = player.getZ();
        // Zoomed out (cartography table + paper): one pixel covers step x step blocks and shows the block at its middle.
        int step = 1 << data.scale;
        int mapMinX = data.centerX - 64 * step;
        int mapMinZ = data.centerZ - 64 * step;
        // What the map remembers per pixel is saved with the world (CaveMapMemory), so a restart changes nothing.
        CaveMapMemory memory = CaveMapMemory.of(level, id);
        java.util.BitSet structurePixels = memory.structure;
        int[] floors = memory.floors;
        boolean memoryChanged = false;
        byte wall = wallColor(kind);
        int layerMin = kind.drawMinY(layer);
        int layerTop = kind.shadeTopY(layer);
        int layerMax = kind.drawMaxY(layer);
        int floorLimit = level.getMinY(); // the floor search goes below the layer too (darker the deeper), see heightShaded
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // The ray heights, lowest first (the lower ray wins, so the upper ones only matter where the lower ones found no open column and are cast on first need):
        // the eye, RAY_OFFSET above it, and LOOK_OFFSET above the block the player looks at.
        int lowest = Math.max(kind.drawMinY(layer), level.getMinY());
        int highest = Math.min(kind.drawMaxY(layer), level.getMaxY()) - 1;
        java.util.TreeSet<Integer> rayYs = new java.util.TreeSet<>();
        rayYs.add(Mth.clamp(head.getY(), lowest, highest));
        rayYs.add(Mth.clamp(head.getY() + RAY_OFFSET, lowest, highest));
        if (player.pick(RADIUS, 1.0F, false) instanceof net.minecraft.world.phys.BlockHitResult looked && looked.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            rayYs.add(Mth.clamp(looked.getBlockPos().getY() + LOOK_OFFSET, lowest, highest));
        }
        int heights = rayYs.size();
        boolean upperWins = kind.upperWins(layer);
        double[][] reach = new double[heights][];
        int[] startY = new int[heights];
        boolean[] reachable = new boolean[heights];
        int index0 = 0;
        for (int rayY : rayYs) {
            int h = index0++;
            startY[h] = rayY;
            // A ray at another height only counts if nothing solid stands between the head and its height - otherwise it would run through another cave behind the rock.
            reachable[h] = true;
            for (int y = Math.min(head.getY(), rayY); y <= Math.max(head.getY(), rayY) && reachable[h]; y++) {
                reachable[h] = passable(level, pos.set(head.getX(), y, head.getZ()));
            }
        }

        int minPixelX = Math.max(0, Math.floorDiv(Mth.floor(originX - RADIUS) - mapMinX, step));
        int maxPixelX = Math.min(127, Math.floorDiv(Mth.floor(originX + RADIUS) - mapMinX, step));
        int minPixelZ = Math.max(0, Math.floorDiv(Mth.floor(originZ - RADIUS) - mapMinZ, step));
        int maxPixelZ = Math.min(127, Math.floorDiv(Mth.floor(originZ + RADIUS) - mapMinZ, step));
        for (int pixelX = minPixelX; pixelX <= maxPixelX; pixelX++) {
            for (int pixelZ = minPixelZ; pixelZ <= maxPixelZ; pixelZ++) {
                int blockX = mapMinX + pixelX * step + step / 2;
                int blockZ = mapMinZ + pixelZ * step + step / 2;
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
                for (int order = 0; order < heights && color == MapColor.NONE; order++) {
                    int h = upperWins ? heights - 1 - order : order; // the lower ray first - the Top Nether Map: the higher one
                    if (!reachable[h]) {
                        continue;
                    }
                    if (kind.usesRays(layer)) {
                        if (reach[h] == null) {
                            reach[h] = castRays(level, pos, originX, originZ, startY[h]);
                        }
                        if (distance > reachAt(reach[h], dx, dz)) {
                            continue;
                        }
                    } else if (distance > RADIUS) {
                        continue; // no rays (top Nether Map): every column in the radius is looked at, walls or not
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
                            exempt = keepsItsColour(state, kind);
                            structure = state.is(STRUCTURE_BLOCKS);
                            boolean fixesWrongLayer = floorY >= layerMin && floorY < layerMax && floors[index] != CaveMapMemory.NO_FLOOR && (floors[index] < layerMin || floors[index] >= layerMax);
                            if (!structure && !fixesWrongLayer && floors[index] != CaveMapMemory.NO_FLOOR && (upperWins ? startY[h] < floors[index] : startY[h] > floors[index])) {
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
                            && isSkyBlack(kind, floorY, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ));
                    packed = skyBlack ? wall : exempt ? color.getPackedId(MapColor.Brightness.NORMAL) : heightShaded(kind, color, floorY, layerMin, layerTop);
                } else {
                    if (seen && !open) {
                        packed = wall; // inside the view and solid at every height: a wall
                    } else if (!seen && existing == 0 && distance <= BLACKOUT_RADIUS) {
                        packed = wall; // beyond where the rays ended: still unknown near the player turns black
                    } else {
                        continue; // an open column without a floor in this layer, or outside the blackout circle, stays untouched
                    }
                }
                if (color != MapColor.NONE && !structure && (floorY < layerMin || floorY >= layerMax) && existing != 0 && existing != wall) {
                    continue; // a floor outside the map's own height range (a deep cave seen on a shallow map) only fills gaps, it never overwrites
                }
                if (packed == wall) {
                    // Black never paints over a discovered pixel.
                    if (existing != 0) {
                        continue;
                    }
                }
                data.updateColor(pixelX, pixelZ, packed);
                if (color != MapColor.NONE && !skyBlack) {
                    floors[index] = floorY;
                    memoryChanged = true;
                    if (structure) {
                        structurePixels.set(index);
                    }
                }
            }
        }
        if (memoryChanged) {
            memory.setDirty();
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

    /** Wood (mine shafts and buildings) and, in the Overworld, lava (a danger sign) are not shaded by height - they keep their normal map colour; Nether lava is shaded like the rest. */
    private static boolean keepsItsColour(BlockState state, CaveMapKind kind) {
        SoundType sound = state.getSoundType();
        return (kind == CaveMapKind.CAVE && state.getFluidState().is(FluidTags.LAVA)) || sound == SoundType.WOOD || sound == SoundType.NETHER_WOOD || sound == SoundType.CHERRY_WOOD
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
     * The floor colour by its height in the map's layer: the base colour in its normal brightness, made HEIGHT_SHADE darker at the layer's lowest block and HEIGHT_BRIGHTEN brighter at
     * its top (CaveMapKind.shadeTopY), linear between. Below the layer: HEIGHT_SHADE plus DARKEN_PER_BLOCK for every block further down - a deep cave on a shallow map fades
     * away, down to MIN_BRIGHTNESS. A map pixel can only be one of the Vanilla palette colours, so the result is the palette colour nearest (in RGB) to the shifted colour - no dithering; the darkness stops at 90 %. Results are cached per base colour and shift (in 1 % steps).
     */
    public static byte heightShaded(CaveMapKind kind, MapColor color, int floorY, int layerMin, int layerTop) {
        double darker = kind == CaveMapKind.NETHER ? NETHER_SHADE : HEIGHT_SHADE;
        double brighter = kind == CaveMapKind.NETHER ? NETHER_BRIGHTEN : HEIGHT_BRIGHTEN;
        double shift;
        if (floorY >= layerMin) {
            double t = Mth.clamp((floorY - layerMin) / (double) Math.max(1, layerTop - layerMin), 0.0, 1.0);
            shift = -darker + (darker + brighter) * t;
        } else {
            shift = -(darker + (layerMin - floorY) * DARKEN_PER_BLOCK);
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
