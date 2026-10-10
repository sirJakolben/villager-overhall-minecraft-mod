package com.smartgolem.test;

import com.smartgolem.SmartGolemMod;
import com.smartgolem.ai.GolemPathNavigation;
import com.smartgolem.ai.GolemPatrolStrollGoal;
import com.smartgolem.climb.GolemClimb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/**
 * Gametests of Smart Iron Golem (run headless with ./gradlew :smart-golem:runGameTestServer): the AI is set up, the pathfinder plans through a wall
 * two blocks high (and not through one of one or three), the golem climbs it, climbs out of a hole and follows an iron ingot. The test area is built
 * by the tests themselves (a stone floor, y = 0 relative to the test, air above).
 */
@EventBusSubscriber(modid = SmartGolemMod.MODID)
public final class GolemGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SmartGolemMod.MODID);

    private static final int SIZE = 16;
    /** Space between the tests (blocks): their areas are built by the tests and must not touch. */
    private static final int PADDING = 24;
    /** Ticks the golem needs to be placed in a ticking chunk and land on the floor - Vanilla's navigation plans no path before that. */
    private static final int WARM_UP = 40;

    static {
        FUNCTIONS.register("ai_setup", () -> GolemGameTests::aiSetup);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            FUNCTIONS.register("plan_over_wall_2_" + side.getName(), () -> helper -> planOverWall2(helper, side));
            FUNCTIONS.register("climb_wall_" + side.getName(), () -> helper -> climbWall(helper, side));
        }
        FUNCTIONS.register("plan_no_wall_3", () -> GolemGameTests::planNoWall3);
        FUNCTIONS.register("plan_no_climb_1", () -> GolemGameTests::planNoClimb1);
        FUNCTIONS.register("climb_out_of_hole", () -> GolemGameTests::climbOutOfHole);
        FUNCTIONS.register("climb_in_water", () -> GolemGameTests::climbInWater);
        FUNCTIONS.register("tempt_ingot", () -> GolemGameTests::temptIngot);
        FUNCTIONS.register("tempt_ingot_night", () -> GolemGameTests::temptIngotNight);
        FUNCTIONS.register("tempt_ingot_survival", () -> GolemGameTests::temptIngotSurvival);
        FUNCTIONS.register("patrol_goes_to_path_block", () -> GolemGameTests::patrolGoesToPathBlock);
    }

    private GolemGameTests() {
    }

    /** Called from the mod's constructor. */
    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        Identifier structure = Identifier.withDefaultNamespace("empty");
        register(event, environment, structure, "ai_setup", 120);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            register(event, environment, structure, "plan_over_wall_2_" + side.getName(), 120);
            register(event, environment, structure, "climb_wall_" + side.getName(), 600);
        }
        register(event, environment, structure, "plan_no_wall_3", 120);
        register(event, environment, structure, "plan_no_climb_1", 120);
        register(event, environment, structure, "climb_out_of_hole", 600);
        register(event, environment, structure, "climb_in_water", 600);
        register(event, environment, structure, "tempt_ingot", 200);
        register(event, environment, structure, "tempt_ingot_night", 300);
        register(event, environment, structure, "tempt_ingot_survival", 300);
        register(event, environment, structure, "patrol_goes_to_path_block", 1200);
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, Identifier structure, String name, int maxTicks) {
        event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                new TestData<>(environment, structure, maxTicks, 0, true, Rotation.NONE, false, 1, 1, false, PADDING)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SmartGolemMod.MODID, path);
    }

    // ---- the test area ----

    /** A stone floor at y = 0 (three blocks thick), air from y = 1 to 8, over SIZE x SIZE blocks, shut in by a border. */
    private static void area(GameTestHelper helper) {
        area(helper, SIZE);
    }

    private static void area(GameTestHelper helper, int size) {
        // The chunks of the area are forced: loaded and ticking, so the golems move.
        BlockPos low = helper.absolutePos(new BlockPos(-2, 0, -2));
        BlockPos high = helper.absolutePos(new BlockPos(size + 2, 0, size + 2));
        for (int chunkX = Math.min(low.getX(), high.getX()) >> 4; chunkX <= Math.max(low.getX(), high.getX()) >> 4; chunkX++) {
            for (int chunkZ = Math.min(low.getZ(), high.getZ()) >> 4; chunkZ <= Math.max(low.getZ(), high.getZ()) >> 4; chunkZ++) {
                helper.getLevel().setChunkForced(chunkX, chunkZ, true);
            }
        }
        for (int x = -1; x <= size; x++) {
            for (int z = -1; z <= size; z++) {
                boolean border = x < 0 || z < 0 || x >= size || z >= size;
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, -1, z, Blocks.STONE);
                helper.setBlock(x, -2, z, Blocks.STONE);
                // A border five blocks high shuts the area in: no way round the walls the tests build.
                for (int y = 1; y <= 8; y++) {
                    helper.setBlock(x, y, z, border && y <= 5 ? Blocks.OBSIDIAN : Blocks.AIR);
                }
            }
        }
    }
    /** A wall across the whole area at x = 7, from y = 1 up to the given height (blocks high). */
    private static void wall(GameTestHelper helper, int height) {
        wall(helper, height, Direction.EAST);
    }

    /** A wall across the whole area, at x = 7 for a golem walking east or west, at z = 7 for one walking south or north. */
    private static void wall(GameTestHelper helper, int height, Direction side) {
        for (int along = 0; along < SIZE; along++) {
            for (int y = 1; y <= height; y++) {
                if (side.getAxis() == Direction.Axis.X) {
                    helper.setBlock(7, y, along, Blocks.STONE);
                } else {
                    helper.setBlock(along, y, 7, Blocks.STONE);
                }
            }
        }
    }

    /** The golem starts 4 cells before the wall, the target is 4 cells behind it (relative to the test). */
    private static BlockPos startFor(Direction side) {
        return switch (side) {
            case EAST -> new BlockPos(4, 1, 8);
            case WEST -> new BlockPos(11, 1, 8);
            case SOUTH -> new BlockPos(8, 1, 4);
            default -> new BlockPos(8, 1, 11);
        };
    }

    private static BlockPos goalFor(Direction side) {
        return switch (side) {
            case EAST -> new BlockPos(11, 1, 8);
            case WEST -> new BlockPos(4, 1, 8);
            case SOUTH -> new BlockPos(8, 1, 11);
            default -> new BlockPos(8, 1, 4);
        };
    }

    private static IronGolem golem(GameTestHelper helper, int x, int y, int z) {
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, x, y, z);
        golem.setPersistenceRequired();
        return golem;
    }

    private static String describe(Path path, IronGolem golem, GameTestHelper helper) {
        StringBuilder sb = new StringBuilder("golem=" + golem.blockPosition() + " pos=" + golem.position() + " path=");
        if (path == null) {
            return sb.append("null").toString();
        }
        sb.append(path.canReach() ? "reach " : "partial ");
        for (int i = 0; i < path.getNodeCount(); i++) {
            sb.append(path.getNode(i).x).append(',').append(path.getNode(i).y).append(',').append(path.getNode(i).z).append(' ');
        }
        sb.append("\ncolumn:");
        for (int dz = 0; dz >= -10; dz--) {
            BlockPos column = golem.blockPosition().offset(0, 0, dz);
            sb.append(" z").append(dz).append("[");
            for (int dy = -1; dy <= 3; dy++) {
                sb.append(helper.getLevel().getBlockState(column.above(dy)).getBlock() == Blocks.AIR ? '.' : '#');
            }
            sb.append("]");
        }
        return sb.toString();
    }

    /** True if two consecutive nodes of the path differ by two blocks in height: a climb edge. */
    private static boolean hasClimbEdge(Path path) {
        for (int i = 1; i < path.getNodeCount(); i++) {
            if (path.getNode(i).y - path.getNode(i - 1).y >= 2) {
                return true;
            }
        }
        return false;
    }

    // ---- the tests ----

    private static void aiSetup(GameTestHelper helper) {
        area(helper);
        IronGolem golem = golem(helper, 3, 1, 3);
        helper.assertTrue(golem.getNavigation() instanceof GolemPathNavigation, "the golem has the smart navigation");
        helper.assertTrue(Math.abs(golem.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY) - 0.1) < 1.0E-6, "water efficiency 0.1, is "
                + golem.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY));
        helper.assertTrue(Math.abs(golem.getAttributeValue(Attributes.TEMPT_RANGE) - 10.0) < 1.0E-6, "tempt range 10");
        helper.assertTrue(golem.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof GolemPatrolStrollGoal), "patrol stroll goal");
        helper.assertTrue(golem.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getGoal() instanceof TemptGoal), "tempt goal");
        helper.succeed();
    }

    /** Plans a path from the golem to the target (relative to the test) once the golem has landed; the result is in the returned array when the tick has come. */
    private static Path[] plan(GameTestHelper helper, IronGolem golem, BlockPos target, boolean[] planned) {
        Path[] result = {null};
        BlockPos absolute = helper.absolutePos(target);
        helper.onEachTick(() -> {
            if (helper.getTick() == WARM_UP) {
                // Reach range 1 like the moveTo of the goals: a golem's nodes are corners of its footprint, the exact cell is not always one of them.
                result[0] = golem.getNavigation().createPath(absolute, 1);
                planned[0] = true;
            }
        });
        return result;
    }

    private static void planOverWall2(GameTestHelper helper, Direction side) {
        area(helper);
        wall(helper, 2, side);
        BlockPos start = startFor(side);
        IronGolem golem = golem(helper, start.getX(), start.getY(), start.getZ());
        boolean[] planned = {false};
        Path[] path = plan(helper, golem, goalFor(side), planned);
        helper.succeedWhen(() -> {
            helper.assertTrue(planned[0], "planned");
            helper.assertTrue(path[0] != null && path[0].canReach(), "a path over a wall two blocks high exists (" + side + ") " + describe(path[0], golem, helper));
            helper.assertTrue(hasClimbEdge(path[0]), "the path has a climb edge (" + side + ")");
        });
    }
    private static void planNoWall3(GameTestHelper helper) {
        area(helper);
        wall(helper, 3);
        IronGolem golem = golem(helper, 4, 1, 8);
        boolean[] planned = {false};
        Path[] path = plan(helper, golem, new BlockPos(11, 1, 8), planned);
        helper.succeedWhen(() -> {
            helper.assertTrue(planned[0], "planned");
            helper.assertTrue(path[0] == null || !path[0].canReach(), "no way over a wall three blocks high");
        });
    }

    private static void planNoClimb1(GameTestHelper helper) {
        area(helper);
        wall(helper, 1);
        IronGolem golem = golem(helper, 4, 1, 8);
        boolean[] planned = {false};
        Path[] path = plan(helper, golem, new BlockPos(11, 1, 8), planned);
        helper.succeedWhen(() -> {
            helper.assertTrue(planned[0], "planned");
            helper.assertTrue(path[0] != null && path[0].canReach(), "a step of one block is walked");
            helper.assertTrue(!hasClimbEdge(path[0]), "and not climbed");
        });
    }
    private static void climbWall(GameTestHelper helper, Direction side) {
        area(helper);
        wall(helper, 2, side);
        BlockPos start = startFor(side);
        IronGolem golem = golem(helper, start.getX(), start.getY(), start.getZ());
        BlockPos target = helper.absolutePos(goalFor(side));
        Vec3 wallMiddle = helper.absoluteVec(new Vec3(8.0, 0.0, 8.0));
        boolean[] climbed = {false};
        helper.onEachTick(() -> {
            if (helper.getTick() == WARM_UP) {
                golem.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
            }
            if (golem.getData(GolemClimb.ATTACHMENT).active()) {
                climbed[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(climbed[0], "the golem climbed (" + side + ")");
            // Beyond the wall: on the target's side of its middle line (the wall is the line 7..8 of the axis, the test may be rotated, so by distance to the target).
            helper.assertTrue(golem.position().distanceTo(Vec3.atBottomCenterOf(target)) < 5.0, "and is on the target's side of the wall (" + side + ")");
        });
    }
    private static void climbOutOfHole(GameTestHelper helper) {
        area(helper);
        // A hole 4 x 4 and two blocks deep (the floor is three blocks thick): its bottom is the third block.
        for (int x = 4; x < 8; x++) {
            for (int z = 6; z < 10; z++) {
                helper.setBlock(x, 0, z, Blocks.AIR);
                helper.setBlock(x, -1, z, Blocks.AIR);
            }
        }
        IronGolem golem = golem(helper, 5, -1, 7);
        BlockPos target = helper.absolutePos(new BlockPos(12, 1, 8));
        double groundY = helper.absoluteVec(new Vec3(0.0, 0.5, 0.0)).y;
        boolean[] climbed = {false};
        helper.onEachTick(() -> {
            if (helper.getTick() == WARM_UP) {
                golem.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
            }
            if (golem.getData(GolemClimb.ATTACHMENT).active()) {
                climbed[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(climbed[0], "the golem climbed");
            helper.assertTrue(golem.getY() > groundY, "and is out of the hole");
        });
    }
    private static void climbInWater(GameTestHelper helper) {
        area(helper);
        // The same wall, standing in water up to the second block of the wall.
        wall(helper, 2);
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < SIZE; z++) {
                helper.setBlock(x, 1, z, Blocks.WATER);
            }
        }
        IronGolem golem = golem(helper, 4, 1, 8);
        boolean[] planned = {false};
        Path[] path = plan(helper, golem, new BlockPos(11, 1, 8), planned);
        helper.succeedWhen(() -> {
            helper.assertTrue(planned[0], "planned");
            helper.assertTrue(path[0] != null && path[0].canReach(), "a path out of the water over the wall exists");
            helper.assertTrue(hasClimbEdge(path[0]), "with a climb edge");
        });
    }
    private static void temptIngot(GameTestHelper helper) {
        area(helper);
        IronGolem golem = golem(helper, 2, 1, 8);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(11.5, 1.0, 8.5)));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT));
        helper.succeedWhen(() -> helper.assertTrue(golem.distanceTo(player) < 5.0F, "the golem has come to the player with the ingot"));
    }

    /** The lure with a survival-mode player (the others use the mock player's default, creative). */
    private static void temptIngotSurvival(GameTestHelper helper) {
        area(helper);
        IronGolem golem = golem(helper, 2, 1, 8);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.snapTo(helper.absoluteVec(new Vec3(11.5, 1.0, 8.5)));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT));
        helper.succeedWhen(() -> helper.assertTrue(golem.distanceTo(player) < 5.0F, "the golem has come to the survival player with the ingot, is at " + golem.blockPosition()
                + " goals " + golem.goalSelector.getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getGoal().toString() + "@" + g.getPriority()).toList()));
    }

    /** The same by night, with a path block to patrol to: the lure must beat the patrol (priority 3 against 4). */
    private static void temptIngotNight(GameTestHelper helper) {
        area(helper, 32);
        helper.setBlock(28, 0, 8, Blocks.DIRT_PATH);
        IronGolem golem = golem(helper, 4, 1, 8);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new Vec3(12.5, 1.0, 8.5)));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT));
        helper.onEachTick(() -> {
            if (helper.getTick() == 1) {
                var server = helper.getLevel().getServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set midnight");
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(golem.distanceTo(player) < 5.0F, "the golem has come to the player with the ingot, is at " + golem.blockPosition()
                + " goals " + golem.goalSelector.getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getGoal().toString() + "@" + g.getPriority()).toList()));
    }

    /**
     * At night the golem walks towards the dirt path block that is 34 blocks away (the patrol map finds it in the loaded chunks around). A point nearer than
     * 16 blocks is no new destination, so it stops about there and strolls as Vanilla does.
     */
    private static void patrolGoesToPathBlock(GameTestHelper helper) {
        int size = 48;
        area(helper, size);
        helper.setBlock(36, 0, 24, Blocks.DIRT_PATH);
        helper.setBlock(37, 0, 24, Blocks.DIRT_PATH);
        IronGolem golem = golem(helper, 2, 1, 24);
        BlockPos pathBlock = helper.absolutePos(new BlockPos(36, 0, 24));
        helper.onEachTick(() -> {
            if (helper.getTick() == 1) {
                var server = helper.getLevel().getServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set midnight");
            }
        });
        helper.succeedWhen(() -> helper.assertTrue(golem.blockPosition().distSqr(pathBlock) < 20.0 * 20.0, "the golem walked towards the path block, is at " + golem.blockPosition()
                + " and the path block at " + pathBlock));
    }
}
