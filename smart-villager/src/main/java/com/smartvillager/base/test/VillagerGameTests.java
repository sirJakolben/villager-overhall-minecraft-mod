package com.smartvillager.base.test;

import com.smartvillager.base.SmartVillagerMod;
import com.smartvillager.base.claim.ManualClaims;
import com.smartvillager.base.mixin.BrewingStandAccessor;
import com.smartvillager.base.trading.TradingBlockCall;
import com.smartvillager.base.trading.TradingBlockLinks;
import com.smartvillager.base.trading.TradingBlocks;
import com.smartvillager.base.world.BrewingStandSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Gametests of Smart Villager (run headless with ./gradlew :smart-villager:runGameTestServer): the weakness cure, the emerald claim of a
 * Vanilla workplace and of a Trading Block, the Trading Block call by redstone, and the fuel of generated brewing stands. Chunk generation
 * and natural zombie spawns need a real world and are not covered here.
 */
@EventBusSubscriber(modid = SmartVillagerMod.MODID)
public final class VillagerGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SmartVillagerMod.MODID);
    private static final int PADDING = 24;

    static {
        FUNCTIONS.register("cure_by_weakness", () -> VillagerGameTests::cureByWeakness);
        FUNCTIONS.register("no_cure_with_golden_apple_alone", () -> helper -> noCureWith(helper, Items.GOLDEN_APPLE));
        FUNCTIONS.register("no_cure_with_golden_carrot", () -> helper -> noCureWith(helper, Items.GOLDEN_CARROT));
        FUNCTIONS.register("claim_vanilla_workplace", () -> VillagerGameTests::claimVanillaWorkplace);
        FUNCTIONS.register("claim_trading_block_and_call", () -> VillagerGameTests::claimTradingBlockAndCall);
        FUNCTIONS.register("brewing_stand_fuel", () -> VillagerGameTests::brewingStandFuel);
    }

    private VillagerGameTests() {
    }

    /** Called from the mod's constructor. */
    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        Identifier structure = Identifier.withDefaultNamespace("empty");
        for (String name : new String[]{"cure_by_weakness", "no_cure_with_golden_apple_alone", "no_cure_with_golden_carrot",
                "claim_vanilla_workplace", "claim_trading_block_and_call", "brewing_stand_fuel"}) {
            event.registerTest(id(name), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                    new TestData<>(environment, structure, 200, 0, true, Rotation.NONE, false, 1, 1, false, PADDING)));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SmartVillagerMod.MODID, path);
    }

    /** A stone floor at y = 0, air above, 12 x 12. */
    private static void area(GameTestHelper helper) {
        // The chunks of the area are forced: loaded and ticking, so mobs and effects run whatever the test world has around.
        BlockPos low = helper.absolutePos(new BlockPos(-2, 0, -2));
        BlockPos high = helper.absolutePos(new BlockPos(14, 0, 14));
        for (int chunkX = Math.min(low.getX(), high.getX()) >> 4; chunkX <= Math.max(low.getX(), high.getX()) >> 4; chunkX++) {
            for (int chunkZ = Math.min(low.getZ(), high.getZ()) >> 4; chunkZ <= Math.max(low.getZ(), high.getZ()) >> 4; chunkZ++) {
                helper.getLevel().setChunkForced(chunkX, chunkZ, true);
            }
        }
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }

    @SuppressWarnings("removal")
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** The weakness effect alone starts the cure (it takes time, like Vanilla's, and uses up the weakness); when the time is over it is a villager. */
    private static void cureByWeakness(GameTestHelper helper) {
        area(helper);
        ZombieVillager zombie = helper.spawn(EntityType.ZOMBIE_VILLAGER, 4, 1, 4);
        AABB around = new AABB(helper.absolutePos(new BlockPos(4, 1, 4))).inflate(3.0);
        boolean[] timeSkipped = {false};
        helper.runAfterDelay(2, () -> zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200), null));
        helper.onEachTick(() -> {
            if (!timeSkipped[0] && zombie.isConverting()) {
                helper.assertTrue(zombie.isAlive() && helper.getLevel().getEntitiesOfClass(Villager.class, around).isEmpty(), "it is still a zombie villager while it is cured");
                helper.assertTrue(!zombie.hasEffect(MobEffects.WEAKNESS), "the cure used up the weakness");
                // Not the 3 to 5 minutes of the test: the last tick.
                zombie.setVillagerConversionTime(1);
                timeSkipped[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(timeSkipped[0], "the cure started");
            helper.assertTrue(!zombie.isAlive() && !helper.getLevel().getEntitiesOfClass(Villager.class, around).isEmpty(), "and ends in a villager");
        });
    }

    /** Golden food does not cure any more - not even a golden apple. */
    private static void noCureWith(GameTestHelper helper, net.minecraft.world.item.Item food) {
        area(helper);
        ZombieVillager zombie = helper.spawn(EntityType.ZOMBIE_VILLAGER, 4, 1, 4);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(food, 2));
        helper.runAfterDelay(2, () -> {
            player.interactOn(zombie, InteractionHand.MAIN_HAND, zombie.position());
            helper.assertTrue(!zombie.isConverting(), "zombie villager does not start converting with " + food);
            helper.succeed();
        });
    }

    private static void claimVanillaWorkplace(GameTestHelper helper) {
        area(helper);
        Villager villager = helper.spawn(EntityType.VILLAGER, 2, 1, 2);
        BlockPos lectern = new BlockPos(6, 1, 6);
        helper.setBlock(lectern, Blocks.LECTERN);
        helper.runAfterDelay(5, () -> {
            var level = helper.getLevel();
            BlockPos abs = helper.absolutePos(lectern);
            Optional<net.minecraft.network.chat.Component> message = ManualClaims.assign(level, villager, abs);
            helper.assertTrue(message.isPresent(), "the claim answers with a message");
            helper.assertTrue(villager.getVillagerData().profession().is(VillagerProfession.LIBRARIAN), "the jobless villager became a librarian");
            helper.assertTrue(villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).equals(Optional.of(GlobalPos.of(level.dimension(), abs))), "the lectern is its job site");
            helper.succeed();
        });
    }

    private static void claimTradingBlockAndCall(GameTestHelper helper) {
        area(helper);
        // Rest comes before the call (a villager at night does not leave its bed): the test runs by day, whatever time the test world has.
        var server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set day");
        Villager villager = helper.spawn(EntityType.VILLAGER, 2, 1, 2);
        villager.setVillagerData(villager.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.FARMER));
        BlockPos station = new BlockPos(3, 1, 3);
        helper.setBlock(station, Blocks.COMPOSTER);
        BlockPos block = new BlockPos(8, 1, 8);
        helper.setBlock(block, TradingBlocks.TRADING_BLOCK.get().defaultBlockState());
        helper.runAfterDelay(30, () -> {
            var level = helper.getLevel();
            BlockPos absStation = helper.absolutePos(station);
            BlockPos absBlock = helper.absolutePos(block);
            villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), absStation));
            ManualClaims.assign(level, villager, absBlock);
            helper.assertTrue(TradingBlockLinks.get(villager).equals(Optional.of(GlobalPos.of(level.dimension(), absBlock))), "the villager remembers its Trading Block");
            helper.assertTrue(!TradingBlockCall.isCalled(villager), "an unpowered block does not call");
            helper.setBlock(new BlockPos(9, 1, 8), Blocks.REDSTONE_BLOCK);
            helper.assertTrue(TradingBlockCall.isCalled(villager), "a powered block calls its villager: signal=" + level.hasNeighborSignal(absBlock) + " calledTo=" + TradingBlockCall.calledTo(villager) + " sleeping=" + villager.isSleeping() + " baby=" + villager.isBaby() + " activities=" + villager.getBrain().getActiveActivities() + " blockState=" + level.getBlockState(absBlock));
            helper.succeed();
        });
    }

    private static void brewingStandFuel(GameTestHelper helper) {
        area(helper);
        int withPowder = 0;
        int runs = 200;
        for (int i = 0; i < runs; i++) {
            helper.setBlock(0, 1, 0, Blocks.BREWING_STAND);
            BrewingStandBlockEntity stand = (BrewingStandBlockEntity) helper.getBlockEntity(new BlockPos(0, 1, 0), BrewingStandBlockEntity.class);
            stand.setItem(4, ItemStack.EMPTY);
            BrewingStandSpawns.fill(helper.getLevel(), stand);
            int fuel = ((BrewingStandAccessor) stand).smartvillager$getFuel();
            helper.assertTrue(fuel >= BrewingStandSpawns.FUEL_MIN && fuel <= BrewingStandSpawns.FUEL_MAX, "fuel between half and full: " + fuel);
            ItemStack slot = stand.getItem(4);
            helper.assertTrue(slot.isEmpty() || (slot.is(Items.BLAZE_POWDER) && slot.getCount() == 1), "the fuel slot is empty or one blaze powder: " + slot);
            if (!slot.isEmpty()) {
                withPowder++;
            }
        }
        helper.assertTrue(withPowder > runs * 0.35 && withPowder < runs * 0.65, "about half of the stands hold blaze powder: " + withPowder + "/" + runs);
        helper.succeed();
    }
}
