package com.villageroverhaul.traderework.mapping.test;

import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.TradeReworkRegistries;
import com.villageroverhaul.traderework.cartographer.CartographerItems;
import com.villageroverhaul.traderework.mapping.MapPool;
import com.villageroverhaul.traderework.mapping.MapStationBlockEntity;
import com.villageroverhaul.traderework.mapping.MappingBlocks;
import com.villageroverhaul.traderework.cavemap.CaveMapDrawer;
import com.villageroverhaul.traderework.cavemap.CaveMapItems;
import com.villageroverhaul.traderework.cavemap.CaveMapMemory;
import com.villageroverhaul.traderework.cavemap.CaveLayer;
import com.villageroverhaul.traderework.cavemap.CaveMapKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/**
 * Gametests of the map pools and stations (headless: ./gradlew :trade-rework:runGameTestServer): the shipped pool files load, and a
 * station takes only its own blank map and reports work only with one waiting. The structure search itself needs a world with structures
 * and is checked in the real game.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class MapGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, TradeReworkMod.MODID);

    static {
        FUNCTIONS.register("map_pools_load", () -> MapGameTests::poolsLoad);
        FUNCTIONS.register("map_station_inventory", () -> MapGameTests::stationInventory);
        FUNCTIONS.register("cave_map_use", () -> MapGameTests::caveMapUse);
        FUNCTIONS.register("cave_map_height_shading", () -> MapGameTests::caveMapHeightShading);
        FUNCTIONS.register("cave_map_surface_black", () -> MapGameTests::caveMapSurfaceBlack);
        FUNCTIONS.register("cave_map_layers", () -> MapGameTests::caveMapLayers);
        FUNCTIONS.register("cave_map_memory_saves", () -> MapGameTests::caveMapMemorySaves);
    }

    private MapGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("maps"));
        Identifier structure = Identifier.withDefaultNamespace("empty");
        for (String name : new String[]{"map_pools_load", "map_station_inventory", "cave_map_use", "cave_map_height_shading", "cave_map_surface_black", "cave_map_layers", "cave_map_memory_saves"}) {
            event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                    new TestData<>(environment, structure, 20, 0, true, Rotation.NONE, false, 1, 1, false, 1)));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, path);
    }

    private static void poolsLoad(GameTestHelper helper) {
        int adventure = 0;
        int challenge = 0;
        for (MapPool pool : helper.getLevel().registryAccess().lookupOrThrow(TradeReworkRegistries.MAP_POOL)) {
            helper.assertTrue(!pool.entries().isEmpty(), "a pool file has entries");
            for (MapPool.Entry entry : pool.entries()) {
                helper.assertTrue(entry.structures().size() > 0, "every entry resolves to at least one structure");
            }
            if (pool.kind() == MapPool.Kind.ADVENTURE) {
                adventure++;
            } else {
                challenge++;
            }
        }
        helper.assertTrue(adventure >= 1 && challenge >= 1, "both pools ship a file (adventure " + adventure + ", challenge " + challenge + ")");
        helper.succeed();
    }

    private static void stationInventory(GameTestHelper helper) {
        helper.setBlock(0, 1, 0, MappingBlocks.ADVENTURE_STATION.get().defaultBlockState());
        helper.setBlock(2, 1, 0, MappingBlocks.CHALLENGE_STATION.get().defaultBlockState());
        MapStationBlockEntity drafting = (MapStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(0, 1, 0)));
        MapStationBlockEntity challenge = (MapStationBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(2, 1, 0)));
        ItemStack adventureMap = new ItemStack(CartographerItems.ADVENTURE_MAP.get());
        ItemStack challengeMap = new ItemStack(CartographerItems.CHALLENGE_MAP.get());
        helper.assertTrue(drafting.kind() == MapPool.Kind.ADVENTURE && challenge.kind() == MapPool.Kind.CHALLENGE, "each station has its pool");
        helper.assertTrue(drafting.canPlaceItem(0, adventureMap) && !drafting.canPlaceItem(0, challengeMap), "the adventure station takes only adventure maps");
        helper.assertTrue(challenge.canPlaceItem(0, challengeMap) && !challenge.canPlaceItem(0, adventureMap), "the challenge station takes only challenge maps");
        helper.assertTrue(!challenge.canPlaceItem(0, new ItemStack(Items.MAP)), "a Vanilla map is no blank map");
        helper.assertTrue(challenge.canPlaceItem(3, new ItemStack(Items.MAP)), "the output slots take anything from players");
        helper.assertTrue(!drafting.hasWork(helper.getLevel()), "an empty station has no work");
        drafting.setItem(0, adventureMap);
        helper.assertTrue(drafting.hasWork(helper.getLevel()), "a blank map waiting means work");
        helper.succeed();
    }

    /** The Cave Map is not made under the sky or with the Nether Map; in a closed room it becomes a map of that room with black walls. */
    private static void caveMapUse(GameTestHelper helper) {
        for (int x = -1; x <= 11; x++) {
            for (int z = -1; z <= 11; z++) {
                for (int y = 0; y <= 6; y++) {
                    boolean inside = x >= 1 && x <= 9 && z >= 1 && z <= 9 && y >= 1 && y <= 4;
                    boolean roof = y >= 5;
                    helper.setBlock(x, y, z, inside || roof ? Blocks.AIR : Blocks.STONE);
                }
            }
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(5.5, 1.0, 5.5)));
        ItemStack caveMap = new ItemStack(CaveMapItems.CAVE_MAP.get());
        ItemStack netherMap = new ItemStack(CaveMapItems.NETHER_MAP.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, caveMap);
        helper.assertTrue(caveMap.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "no cave map under the open sky");
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                helper.setBlock(x, 5, z, Blocks.STONE);
            }
        }
        helper.assertTrue(netherMap.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "no nether map in the Overworld");
        helper.assertTrue(!netherMap.isEmpty() && !caveMap.isEmpty(), "a refused use keeps the maps");
        InteractionResult result = caveMap.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result instanceof InteractionResult.Success, "the cave map works in a closed room: result " + result + ", sky " + helper.getLevel().canSeeSky(BlockPos.containing(player.getEyePosition())) + ", eye " + player.getEyePosition() + ", dim " + helper.getLevel().dimension());
        ItemStack filled = ((InteractionResult.Success) result).heldItemTransformedTo();
        if (filled == null || !filled.is(CaveMapItems.FILLED_CAVE_MAP.get())) {
            // A creative player keeps the empty map and gets the filled one in the inventory (ItemUtils.createFilledResult).
            filled = player.getInventory().getNonEquipmentItems().stream().filter(s -> s.is(CaveMapItems.FILLED_CAVE_MAP.get())).findFirst().orElse(null);
        }
        helper.assertTrue(filled != null && filled.is(CaveMapItems.FILLED_CAVE_MAP.get()), "it became a filled cave map");
        helper.assertTrue(filled.get(CaveMapItems.CAVE_LAYER.get()) != null, "bound to a layer");
        helper.assertTrue(filled.get(net.minecraft.core.component.DataComponents.ITEM_NAME) != null, "named after its layer once revealed");
        MapItemSavedData data = MapItem.getSavedData(filled, helper.getLevel());
        helper.assertTrue(data != null, "with map data");
        int drawn = 0;
        int black = 0;
        for (byte color : data.colors) {
            if (color != 0) {
                drawn++;
            }
            if (color == MapColor.COLOR_BLACK.getPackedId(MapColor.Brightness.LOWEST)) {
                black++;
            }
        }
        int coloured = drawn - black;
        helper.assertTrue(coloured >= 20 && coloured <= 120, "the room floor is drawn in colour, nothing beyond the walls (the room may straddle the map edge, so not all 81 floor pixels): " + coloured + " coloured pixels");
        helper.assertTrue(black >= 500, "the unknown around the player is black: " + black + " black pixels");
        // Zoomed out at the cartography table (paper): the picture moves to a map with twice as coarse pixels, and it keeps drawing there.
        MapId oldId = filled.get(net.minecraft.core.component.DataComponents.MAP_ID);
        filled.set(net.minecraft.core.component.DataComponents.MAP_POST_PROCESSING, net.minecraft.world.item.component.MapPostProcessing.SCALE);
        filled.getItem().onCraftedPostProcess(filled, helper.getLevel());
        MapId zoomedId = filled.get(net.minecraft.core.component.DataComponents.MAP_ID);
        MapItemSavedData zoomed = MapItem.getSavedData(filled, helper.getLevel());
        helper.assertTrue(zoomedId != null && !zoomedId.equals(oldId) && zoomed != null && zoomed.scale == 1, "zooming out makes a new map one scale up");
        int movedColoured = 0;
        for (byte color : zoomed.colors) {
            if (color != 0 && color != MapColor.COLOR_BLACK.getPackedId(MapColor.Brightness.LOWEST)) {
                movedColoured++;
            }
        }
        helper.assertTrue(movedColoured >= 5, "the explored room moved to the zoomed map: " + movedColoured + " coloured pixels");
        CaveMapDrawer.draw(helper.getLevel(), player, zoomedId, zoomed, CaveMapKind.CAVE, filled.get(CaveMapItems.CAVE_LAYER.get()));
        helper.assertTrue(zoomed.colors.length == 16384, "the zoomed map is drawn without trouble");
        helper.succeed();
    }

    /** The layer's top is bright, its bottom dark, below it fades to black (CaveMapDrawer.heightShaded). */
    private static void caveMapHeightShading(GameTestHelper helper) {
        MapColor stone = MapColor.STONE;
        double normal = luminance(stone.getPackedId(MapColor.Brightness.NORMAL));
        double top = luminance(CaveMapDrawer.heightShaded(CaveMapKind.CAVE, stone, 0, -64, 0));
        helper.assertTrue(top > normal, "the top of the layer is brighter than normal");
        helper.assertTrue(CaveMapDrawer.heightShaded(CaveMapKind.CAVE, stone, -32, -64, 0) == stone.getPackedId(MapColor.Brightness.NORMAL), "the middle of the layer keeps its colour");
        double bottom = luminance(CaveMapDrawer.heightShaded(CaveMapKind.CAVE, stone, -64, -64, 0));
        helper.assertTrue(bottom < normal, "the bottom of the layer is darker");
        double below = luminance(CaveMapDrawer.heightShaded(CaveMapKind.CAVE, stone, -66, -64, 0));
        helper.assertTrue(below < bottom, "below the layer it is darker still");
        helper.assertTrue(luminance(CaveMapDrawer.heightShaded(CaveMapKind.CAVE, stone, -74, -64, 0)) < below, "ten blocks below the layer it is darker still");
        // The Nether: 30 % darker at the bottom of a layer, 50 % brighter at the top, so its middle is already brighter than the normal colour.
        helper.assertTrue(luminance(CaveMapDrawer.heightShaded(CaveMapKind.NETHER, stone, 100, 0, 100)) > top, "the top of a Nether layer is brighter than the top of an Overworld layer");
        helper.assertTrue(luminance(CaveMapDrawer.heightShaded(CaveMapKind.NETHER, stone, 0, 0, 100)) > luminance(CaveMapDrawer.heightShaded(CaveMapKind.CAVE, stone, 0, 0, 100)), "and its bottom is less dark");
        byte deepest = CaveMapDrawer.heightShaded(CaveMapKind.CAVE, stone, -300, -64, 0);
        helper.assertTrue(luminance(deepest) < 16, "far below it is almost black (90 % darker at most)");
        helper.succeed();
    }

    /** Above Y 60 with open sky over the floor it is the surface and black; caves, low ground and the Nether are not. */
    private static void caveMapSurfaceBlack(GameTestHelper helper) {
        helper.assertTrue(CaveMapDrawer.isSkyBlack(CaveMapKind.CAVE, 70, 71), "a grass top at Y 70 with sky above is black");
        helper.assertTrue(!CaveMapDrawer.isSkyBlack(CaveMapKind.CAVE, 70, 90), "a cave floor at Y 70 with rock above is not");
        helper.assertTrue(!CaveMapDrawer.isSkyBlack(CaveMapKind.CAVE, 55, 56), "low ground below Y 60 keeps its colour");
        helper.assertTrue(!CaveMapDrawer.isSkyBlack(CaveMapKind.NETHER, 70, 71), "the Nether has no sky");
        helper.assertTrue(Blocks.MOSSY_COBBLESTONE.defaultBlockState().is(CaveMapDrawer.STRUCTURE_BLOCKS), "dungeon cobblestone is a structure block");
        helper.assertTrue(Blocks.TUFF_BRICKS.defaultBlockState().is(CaveMapDrawer.STRUCTURE_BLOCKS), "trial chamber tuff bricks are a structure block");
        helper.assertTrue(Blocks.OAK_PLANKS.defaultBlockState().is(CaveMapDrawer.STRUCTURE_BLOCKS), "mineshaft wood is a structure block");
        helper.assertTrue(!Blocks.STONE.defaultBlockState().is(CaveMapDrawer.STRUCTURE_BLOCKS), "plain stone is not");
        helper.succeed();
    }

    /** Overworld two layers, Nether four; the Middle and Top Nether Map draw from the lava sea up to Y 105 without rays, the Top one lets the higher floor win. */
    private static void caveMapLayers(GameTestHelper helper) {
        CaveMapKind nether = CaveMapKind.NETHER;
        helper.assertTrue(nether.layerAt(10).get() == CaveLayer.BOTTOM && nether.layerAt(40).get() == CaveLayer.MIDDLE && nether.layerAt(69).get() == CaveLayer.MIDDLE
                && nether.layerAt(70).get() == CaveLayer.TOP && nether.layerAt(105).get() == CaveLayer.TOP && nether.layerAt(106).get() == CaveLayer.CEILING
                && nether.layerAt(127).get() == CaveLayer.CEILING && nether.layerAt(128).isEmpty(), "a Nether map is made as bottom below 32, middle to 69, top 70 to 105, ceiling above 105");
        helper.assertTrue(CaveMapKind.CAVE.layerAt(-10).get() == CaveLayer.BOTTOM && CaveMapKind.CAVE.layerAt(10).get() == CaveLayer.TOP, "the Overworld has two layers");
        helper.assertTrue(nether.drawsAt(CaveLayer.MIDDLE, 105) && !nether.drawsAt(CaveLayer.MIDDLE, 106) && nether.drawsAt(CaveLayer.TOP, 33) && nether.drawsAt(CaveLayer.TOP, 31)
                && !nether.drawsAt(CaveLayer.TOP, 20) && nether.drawsAt(CaveLayer.TOP, 105) && !nether.drawsAt(CaveLayer.TOP, 106), "the Middle and Top Nether Map draw from the lava sea up to Y 105");
        helper.assertTrue(nether.drawsAt(CaveLayer.BOTTOM, 20) && !nether.drawsAt(CaveLayer.BOTTOM, 40) && nether.drawsAt(CaveLayer.CEILING, 110) && !nether.drawsAt(CaveLayer.CEILING, 100),
                "the Nether Cave Maps draw in their own band");
        helper.assertTrue(nether.upperWins(CaveLayer.TOP) && !nether.upperWins(CaveLayer.MIDDLE) && !nether.upperWins(CaveLayer.CEILING) && !CaveMapKind.CAVE.upperWins(CaveLayer.TOP),
                "only the Top Nether Map lets the higher floor win");
        helper.assertTrue(nether.usesRays(CaveLayer.BOTTOM) && nether.usesRays(CaveLayer.CEILING) && !nether.usesRays(CaveLayer.MIDDLE) && !nether.usesRays(CaveLayer.TOP)
                && CaveMapKind.CAVE.usesRays(CaveLayer.TOP) && CaveMapKind.CAVE.usesRays(CaveLayer.BOTTOM), "everything with Cave in its name has rays, nothing else");
        helper.succeed();
    }

    /** What a map remembers per pixel (floor heights, structure locks) survives saving and loading. */
    private static void caveMapMemorySaves(GameTestHelper helper) {
        CaveMapMemory memory = new CaveMapMemory();
        memory.floors[5] = -33;
        memory.floors[16383] = 90;
        memory.structure.set(7);
        memory.structure.set(12000);
        var encoded = CaveMapMemory.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, memory).getOrThrow();
        CaveMapMemory loaded = CaveMapMemory.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(loaded.floors[5] == -33 && loaded.floors[16383] == 90 && loaded.floors[6] == CaveMapMemory.NO_FLOOR, "the floor heights are saved");
        helper.assertTrue(loaded.structure.get(7) && loaded.structure.get(12000) && !loaded.structure.get(8), "the structure locks are saved");
        helper.succeed();
    }

    private static double luminance(byte packed) {
        int argb = MapColor.getColorFromPackedId(packed & 0xFF);
        return 0.3 * net.minecraft.util.ARGB.red(argb) + 0.59 * net.minecraft.util.ARGB.green(argb) + 0.11 * net.minecraft.util.ARGB.blue(argb);
    }
}
