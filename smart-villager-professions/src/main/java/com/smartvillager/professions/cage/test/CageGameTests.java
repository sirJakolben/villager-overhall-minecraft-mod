package com.smartvillager.professions.cage.test;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.cage.Cages;
import com.smartvillager.professions.cage.ZombieCaptives;
import com.smartvillager.professions.cage.ZombieVillagerCageBlock;
import com.smartvillager.professions.cage.ZombieVillagerCageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/**
 * Gametests of the Zombie Villager Cage (headless: ./gradlew :trade-rework:runGameTestServer): a golem carries a zombie villager to the cage, breaking
 * the cage lets it out, a golem hit while carrying drops it, and an empty cage in reach spares the zombie villager from the attack.
 * The area is built by the tests themselves (stone floor at y = 0, shut in by a wall).
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class CageGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SmartVillagerProfessionsMod.MODID);
    private static final int SIZE = 16;
    /** Space between the tests: the cage search reaches 48 blocks, so tests see each other's cages - they are written to cope with that. */
    private static final int PADDING = 24;
    private static final int WARM_UP = 40;

    static {
        FUNCTIONS.register("cage_golem_delivers", () -> CageGameTests::cageGolemDelivers);
        FUNCTIONS.register("cage_break_releases", () -> CageGameTests::cageBreakReleases);
        FUNCTIONS.register("cage_golem_distracted", () -> CageGameTests::cageGolemDistracted);
        FUNCTIONS.register("cage_spares_only_with_cage", () -> CageGameTests::cageSparesOnlyWithCage);
        FUNCTIONS.register("cage_cured_by_weakness_potion", () -> CageGameTests::cageCuredByWeaknessPotion);
        FUNCTIONS.register("lure_with_all_mods", () -> CageGameTests::lureWithAllMods);
    }

    private CageGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        Identifier structure = Identifier.withDefaultNamespace("empty");
        register(event, environment, structure, "cage_golem_delivers", 900);
        register(event, environment, structure, "cage_break_releases", 200);
        register(event, environment, structure, "cage_golem_distracted", 600);
        register(event, environment, structure, "cage_spares_only_with_cage", 300);
        register(event, environment, structure, "cage_cured_by_weakness_potion", 200);
        register(event, environment, structure, "lure_with_all_mods", 300);
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, Identifier structure, String name, int maxTicks) {
        event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                new TestData<>(environment, structure, maxTicks, 0, true, Rotation.NONE, false, 1, 1, false, PADDING)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, path);
    }

    /** A stone floor at y = 0, air from y = 1 to 8, shut in by an obsidian wall; the chunks are forced so everything ticks. */
    private static void area(GameTestHelper helper) {
        BlockPos low = helper.absolutePos(new BlockPos(-2, 0, -2));
        BlockPos high = helper.absolutePos(new BlockPos(SIZE + 2, 0, SIZE + 2));
        for (int chunkX = Math.min(low.getX(), high.getX()) >> 4; chunkX <= Math.max(low.getX(), high.getX()) >> 4; chunkX++) {
            for (int chunkZ = Math.min(low.getZ(), high.getZ()) >> 4; chunkZ <= Math.max(low.getZ(), high.getZ()) >> 4; chunkZ++) {
                helper.getLevel().setChunkForced(chunkX, chunkZ, true);
            }
        }
        for (int x = -1; x <= SIZE; x++) {
            for (int z = -1; z <= SIZE; z++) {
                boolean border = x < 0 || z < 0 || x >= SIZE || z >= SIZE;
                helper.setBlock(x, 0, z, Blocks.STONE);
                helper.setBlock(x, -1, z, Blocks.STONE);
                for (int y = 1; y <= 8; y++) {
                    helper.setBlock(x, y, z, border && y <= 5 ? Blocks.OBSIDIAN : Blocks.AIR);
                }
            }
        }
    }

    private static IronGolem golem(GameTestHelper helper, int x, int y, int z) {
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, x, y, z);
        golem.setPersistenceRequired();
        return golem;
    }


    /** Places a cage (both halves) at the position relative to the test. */
    private static ZombieVillagerCageBlockEntity cage(GameTestHelper helper, int x, int z) {
        helper.setBlock(x, 1, z, Cages.CAGE.get().defaultBlockState());
        helper.setBlock(x, 2, z, Cages.CAGE.get().defaultBlockState().setValue(ZombieVillagerCageBlock.HALF, DoubleBlockHalf.UPPER));
        return (ZombieVillagerCageBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(x, 1, z)));
    }

    private static ZombieVillager zombieVillager(GameTestHelper helper, int x, int z) {
        ZombieVillager zombie = helper.spawn(EntityType.ZOMBIE_VILLAGER, x, 1, z);
        zombie.setPersistenceRequired();
        return zombie;
    }

    /** The golem takes a zombie villager in its arms, carries it to the free cage and puts it in; the golem does not attack it. */
    private static void cageGolemDelivers(GameTestHelper helper) {
        area(helper);
        ZombieVillagerCageBlockEntity cage = cage(helper, 12, 8);
        IronGolem golem = golem(helper, 3, 1, 8);
        ZombieVillager zombie = zombieVillager(helper, 6, 8);
        zombie.setHealth(zombie.getMaxHealth());
        helper.succeedWhen(() -> {
            helper.assertTrue(cage.isOccupied(), "the zombie villager is in the cage; golem at " + golem.blockPosition() + " rel y " + golem.getY() + ", zombie alive=" + zombie.isAlive() + " hp=" + zombie.getHealth() + " at " + zombie.blockPosition() + " vehicle=" + zombie.getVehicle() + " removed=" + zombie.getRemovalReason() + " golem target=" + golem.getTarget() + " lastDamage=" + zombie.getLastDamageSource() + " hurtBy=" + zombie.getLastHurtByMob());
            helper.assertTrue(!zombie.isAlive(), "the zombie villager entity is gone (only data in the cage)");
            helper.assertTrue(helper.getLevel().getBlockState(cage.getBlockPos()).getValue(ZombieVillagerCageBlock.OCCUPIED), "the block shows it is occupied");
            helper.assertTrue(golem.getPassengers().isEmpty(), "the golem's arms are free again");
        });
    }

    /** Breaking the cage lets the prisoner out as a zombie villager again. */
    private static void cageBreakReleases(GameTestHelper helper) {
        area(helper);
        ZombieVillagerCageBlockEntity cage = cage(helper, 8, 8);
        ZombieVillager zombie = zombieVillager(helper, 4, 4);
        AABB around = new AABB(helper.absolutePos(new BlockPos(8, 1, 8))).inflate(3.0);
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(cage.insert(zombie), "the cage takes the zombie villager");
            helper.assertTrue(!zombie.isAlive(), "it is only data now");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ZombieVillager.class, around, ZombieVillager::isAlive).isEmpty(), "nothing walks in the cage");
        });
        helper.runAtTickTime(10, () -> helper.setBlock(8, 1, 8, Blocks.AIR));
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getEntitiesOfClass(ZombieVillager.class, around, ZombieVillager::isAlive).size() == 1,
                "one zombie villager is out again"));
    }

    /** A golem hit by a skeleton while carrying drops the zombie villager (dazed, no AI) and goes for the skeleton. */
    private static void cageGolemDistracted(GameTestHelper helper) {
        area(helper);
        cage(helper, 14, 8);
        IronGolem golem = golem(helper, 3, 1, 8);
        ZombieVillager zombie = zombieVillager(helper, 4, 8);
        Skeleton skeleton = helper.spawn(EntityType.SKELETON, 1, 1, 12);
        skeleton.setPersistenceRequired();
        skeleton.setNoAi(true);
        boolean[] hit = {false};
        String[] seen = {null};
        helper.onEachTick(() -> {
            if (!hit[0] && zombie.getVehicle() == golem) {
                hit[0] = true;
                golem.setLastHurtByMob(skeleton);
            } else if (hit[0] && seen[0] == null && zombie.getVehicle() == null) {
                seen[0] = "dropped, alive=" + zombie.isAlive() + " hp=" + zombie.getHealth() + " removed=" + zombie.getRemovalReason() + " dazed=" + ZombieCaptives.isDazed(zombie) + " noAi=" + zombie.isNoAi() + " target=" + golem.getTarget();
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(hit[0], "the golem carried the zombie villager");
            helper.assertTrue(seen[0] != null, "the zombie villager was dropped");
            helper.assertTrue(seen[0].contains("dazed=true") && seen[0].contains("noAi=true"), "dropped zombie villager is dazed without AI: " + seen[0]);
            helper.assertTrue(golem.getTarget() == skeleton, "the golem turned on the skeleton: " + seen[0] + " now " + golem.getTarget());
        });
    }

    /** An empty cage in reach spares the zombie villager from the golem's attack; one the golems gave up on is attacked again. */
    private static void cageSparesOnlyWithCage(GameTestHelper helper) {
        area(helper);
        IronGolem golem = golem(helper, 3, 1, 8);
        ZombieVillager zombie = zombieVillager(helper, 5, 8);
        cage(helper, 12, 12);
        helper.runAtTickTime(2, () -> {
            helper.assertTrue(!golem.canAttack(zombie), "an empty cage in reach: the zombie villager is spared");
            ZombieCaptives.giveUp(zombie, 600);
            helper.assertTrue(golem.canAttack(zombie), "given up on: the golem attacks it as Vanilla does");
            helper.succeed();
        });
    }

    /** A potion of weakness on the cage starts the cure (no golden apple); breaking the cage in the middle lets the zombie villager out still being cured. */
    private static void cageCuredByWeaknessPotion(GameTestHelper helper) {
        area(helper);
        ZombieVillagerCageBlockEntity cage = cage(helper, 8, 8);
        ZombieVillager zombie = zombieVillager(helper, 4, 4);
        @SuppressWarnings("removal") net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack potion = net.minecraft.world.item.alchemy.PotionContents.createItemStack(net.minecraft.world.item.Items.POTION,
                net.minecraft.world.item.alchemy.Potions.WEAKNESS);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, potion);
        AABB around = new AABB(helper.absolutePos(new BlockPos(8, 1, 8))).inflate(3.0);
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(cage.insert(zombie), "the cage takes the zombie villager");
            helper.assertTrue(cage.interact(player.getMainHandItem(), player, net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction(), "the potion is used on the cage");
            helper.assertTrue(cage.isCuring() && cage.isOccupied(), "the cure has started and the prisoner is still inside");
        });
        helper.runAtTickTime(10, () -> helper.setBlock(8, 1, 8, net.minecraft.world.level.block.Blocks.AIR));
        helper.succeedWhen(() -> helper.assertTrue(helper.getLevel().getEntitiesOfClass(ZombieVillager.class, around, z -> z.isAlive() && z.isConverting()).size() == 1,
                "broken in the middle of the cure, the zombie villager is out and still being cured"));
    }

    /** Smart Iron Golem's iron ingot lure with every mod loaded: the cage goal in front of it must not get in its way. */
    private static void lureWithAllMods(GameTestHelper helper) {
        area(helper);
        IronGolem golem = golem(helper, 2, 1, 8);
        @SuppressWarnings("removal") net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(8.5, 1.0, 8.5)));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        helper.succeedWhen(() -> helper.assertTrue(golem.distanceTo(player) < 5.0F, "the golem has come to the player with the ingot, is at " + golem.blockPosition() + " player at " + player.blockPosition() + " dist " + golem.distanceTo(player)
                + " temptRange " + golem.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.TEMPT_RANGE) + " held " + player.getMainHandItem() + " alive " + player.isAlive() + " spectator " + player.isSpectator() + " level players " + helper.getLevel().players().size() + " goals " + golem.goalSelector.getAvailableGoals().stream().map(g -> g.getGoal() + "@" + g.getPriority() + (g.isRunning() ? "*" : "")).toList()));
    }
}
