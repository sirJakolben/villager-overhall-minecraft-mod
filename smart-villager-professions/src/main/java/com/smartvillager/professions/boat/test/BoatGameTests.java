package com.smartvillager.professions.boat.test;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.boat.BoatState;
import com.smartvillager.professions.boat.BoatUpgrades;
import com.smartvillager.professions.boat.Cannon;
import com.smartvillager.professions.boat.CannonballEntity;
import com.smartvillager.professions.fisherman.FishermanItems;
import com.smartvillager.progression.data.ItemExchange;
import com.smartvillager.progression.data.ModDataPackRegistries;
import com.smartvillager.progression.trade.RequiredEnchantmentCost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/**
 * Gametests of the cannon boat (headless: ./gradlew :smart-villager-professions:runGameTestServer). They also prove that the boat mixins apply:
 * upgrades survive item -> entity -> item, the trade payments accept the right boats and the result keeps the wood, a cannon takes a seat,
 * the shot needs a cannonball, and its explosion leaves the blocks alone.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class BoatGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SmartVillagerProfessionsMod.MODID);
    private static final List<String> NAMES = List.of("boat_upgrades_round_trip", "boat_trade_payments", "cannon_boat_seats", "cannon_needs_ball", "cannon_ball_keeps_blocks");

    static {
        FUNCTIONS.register("boat_upgrades_round_trip", () -> BoatGameTests::upgradesRoundTrip);
        FUNCTIONS.register("boat_trade_payments", () -> BoatGameTests::tradePayments);
        FUNCTIONS.register("cannon_boat_seats", () -> BoatGameTests::seats);
        FUNCTIONS.register("cannon_needs_ball", () -> BoatGameTests::needsBall);
        FUNCTIONS.register("cannon_ball_keeps_blocks", () -> BoatGameTests::ballKeepsBlocks);
    }

    private BoatGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("boat_default"));
        for (String name : NAMES) {
            event.registerTest(id(name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                    new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1, false, 8)));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, path);
    }

    private static ItemStack upgradedBoatItem() {
        ItemStack item = new ItemStack(Items.SPRUCE_BOAT);
        item.set(BoatUpgrades.CANNON.get(), Unit.INSTANCE);
        item.set(BoatUpgrades.MAST.get(), new BoatUpgrades.Mast(new ItemStack(Items.RED_BANNER)));
        return item;
    }

    private static AbstractBoat placeFromItem(GameTestHelper helper, ItemStack item, int x, int y, int z) {
        AbstractBoat boat = (AbstractBoat) EntityType.SPRUCE_BOAT.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        boat.setInitialPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(x, y, z)).x, helper.absoluteVec(new net.minecraft.world.phys.Vec3(x, y, z)).y, helper.absoluteVec(new net.minecraft.world.phys.Vec3(x, y, z)).z);
        EntityType.<AbstractBoat>createDefaultStackConfig(helper.getLevel(), item, null).accept(boat);
        helper.getLevel().addFreshEntity(boat);
        return boat;
    }

    private static void floor(GameTestHelper helper, int sizeX, int sizeZ, net.minecraft.world.level.block.Block block) {
        BlockPos low = helper.absolutePos(new BlockPos(-2, 0, -2));
        BlockPos high = helper.absolutePos(new BlockPos(sizeX + 2, 0, sizeZ + 2));
        for (int chunkX = Math.min(low.getX(), high.getX()) >> 4; chunkX <= Math.max(low.getX(), high.getX()) >> 4; chunkX++) {
            for (int chunkZ = Math.min(low.getZ(), high.getZ()) >> 4; chunkZ <= Math.max(low.getZ(), high.getZ()) >> 4; chunkZ++) {
                helper.getLevel().setChunkForced(chunkX, chunkZ, true);
            }
        }
        for (int x = -1; x <= sizeX; x++) {
            for (int z = -1; z <= sizeZ; z++) {
                helper.setBlock(x, 0, z, block);
                for (int y = 1; y <= 6; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }

    /** A boat item with cannon + mast + banner becomes an entity with the same state, and a destroyed one drops it all again. */
    private static void upgradesRoundTrip(GameTestHelper helper) {
        floor(helper, 12, 12, Blocks.STONE);
        AbstractBoat boat = placeFromItem(helper, upgradedBoatItem(), 6, 1, 6);
        BoatState state = BoatUpgrades.state(boat);
        helper.assertTrue(state.cannon() && state.mast() && state.banner().is(Items.RED_BANNER), "the placed boat took cannon, mast and banner: " + state);
        boat.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 5.0F);
        AABB around = new AABB(boat.blockPosition()).inflate(4.0);
        helper.succeedWhen(() -> {
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, drop -> drop.getItem().is(Items.SPRUCE_BOAT));
            helper.assertTrue(drops.size() == 1, "the boat dropped as one item: " + drops.size());
            ItemStack dropped = drops.get(0).getItem();
            BoatUpgrades.Mast mast = dropped.get(BoatUpgrades.MAST.get());
            helper.assertTrue(dropped.has(BoatUpgrades.CANNON.get()) && mast != null && mast.banner().is(Items.RED_BANNER), "the dropped boat keeps its upgrades: " + dropped);
        });
    }

    /** The cannon offer accepts a plain boat of any wood, no chest boat and no boat that has a cannon; the result is that boat upgraded. */
    private static void tradePayments(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ItemExchange cannonOffer = registries.lookupOrThrow(ModDataPackRegistries.EXCHANGE).getValue(id("fisherman_cannon_boat"));
        helper.assertTrue(cannonOffer != null && cannonOffer.secondInput().isPresent(), "the cannon boat entry loaded with a boat payment");
        ItemCost payment = RequiredEnchantmentCost.of(cannonOffer.secondInput().get(), registries);
        helper.assertTrue(payment.test(new ItemStack(Items.SPRUCE_BOAT)), "a spruce boat pays");
        helper.assertTrue(payment.test(new ItemStack(Items.BAMBOO_RAFT)), "a raft pays");
        helper.assertTrue(!payment.test(new ItemStack(Items.OAK_CHEST_BOAT)), "a chest boat does not pay for a cannon");
        helper.assertTrue(!payment.test(upgradedBoatItem()), "a boat that has a cannon does not pay again");
        helper.assertTrue(!payment.test(new ItemStack(Items.OAK_PLANKS)), "planks do not pay");

        ItemExchange mastOffer = registries.lookupOrThrow(ModDataPackRegistries.EXCHANGE).getValue(id("fisherman_boat_mast"));
        ItemCost mastPayment = RequiredEnchantmentCost.of(mastOffer.secondInput().get(), registries);
        helper.assertTrue(mastPayment.test(new ItemStack(Items.OAK_CHEST_BOAT)), "a chest boat pays for a mast");

        ItemStack shown = new ItemStack(Items.OAK_BOAT);
        shown.set(BoatUpgrades.CANNON.get(), Unit.INSTANCE);
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD), java.util.Optional.of(payment), shown, 0, 1, 0, 0.0F);
        ItemStack result = BoatUpgrades.transform(offer, new ItemStack(Items.EMERALD, 12), new ItemStack(Items.SPRUCE_BOAT), shown);
        helper.assertTrue(result.is(Items.SPRUCE_BOAT) && result.has(BoatUpgrades.CANNON.get()) && !result.has(BoatUpgrades.MAST.get()), "the result is the paid spruce boat with a cannon: " + result);
        ItemStack plain = new ItemStack(Items.EMERALD);
        helper.assertTrue(BoatUpgrades.transform(offer, new ItemStack(Items.EMERALD), ItemStack.EMPTY, plain) == plain, "other offers pass through untouched");
        helper.succeed();
    }

    /** A boat holds two sheep, a cannon boat one - and a boat without a cannon is unchanged. */
    private static void seats(GameTestHelper helper) {
        floor(helper, 12, 12, Blocks.STONE);
        AbstractBoat plain = placeFromItem(helper, new ItemStack(Items.OAK_BOAT), 3, 1, 3);
        AbstractBoat cannon = placeFromItem(helper, upgradedBoatItem(), 8, 1, 8);
        Sheep a = helper.spawn(EntityType.SHEEP, 3, 1, 3);
        Sheep b = helper.spawn(EntityType.SHEEP, 3, 1, 3);
        Sheep c = helper.spawn(EntityType.SHEEP, 8, 1, 8);
        Sheep d = helper.spawn(EntityType.SHEEP, 8, 1, 8);
        helper.assertTrue(a.startRiding(plain) && b.startRiding(plain), "a plain boat takes two");
        helper.assertTrue(c.startRiding(cannon), "a cannon boat takes one");
        helper.assertTrue(!d.startRiding(cannon), "a cannon boat takes no second one");
        helper.succeed();
    }

    /** Without a cannonball the shot only thuds; with one it fires and uses it up; then the cannon reloads. */
    private static void needsBall(GameTestHelper helper) {
        floor(helper, 12, 12, Blocks.STONE);
        AbstractBoat boat = placeFromItem(helper, upgradedBoatItem(), 6, 1, 3);
        @SuppressWarnings("removal") ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(boat.getX(), boat.getY(), boat.getZ());
        helper.assertTrue(player.startRiding(boat), "the player boards");
        Cannon.fire(boat, 50);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(CannonballEntity.class, boat.getBoundingBox().inflate(6.0)).isEmpty(), "no ball without a cannonball");
        helper.assertTrue(BoatUpgrades.state(boat).cooldown(helper.getLevel().getGameTime()) == 0, "a thud does not start the reload");

        player.getInventory().add(new ItemStack(FishermanItems.CANNONBALL.get(), 2));
        Cannon.fire(boat, 50);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(CannonballEntity.class, boat.getBoundingBox().inflate(6.0)).size() == 1, "one ball in the air");
        helper.assertTrue(player.getInventory().countItem(FishermanItems.CANNONBALL.get()) == 1, "one cannonball used up");
        Cannon.fire(boat, 50);
        helper.assertTrue(player.getInventory().countItem(FishermanItems.CANNONBALL.get()) == 1, "still reloading: no second shot");
        helper.getLevel().getEntitiesOfClass(CannonballEntity.class, boat.getBoundingBox().inflate(6.0)).forEach(CannonballEntity::discard);
        helper.succeed();
    }

    /** The ball lands in a floor of dirt (which TNT would crater) and explodes - the dirt stays. */
    private static void ballKeepsBlocks(GameTestHelper helper) {
        floor(helper, 30, 30, Blocks.DIRT);
        AbstractBoat boat = placeFromItem(helper, upgradedBoatItem(), 15, 1, 2);
        @SuppressWarnings("removal") ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        player.setPos(boat.getX(), boat.getY(), boat.getZ());
        helper.assertTrue(player.startRiding(boat), "the player boards");
        Cannon.fire(boat, 1);
        AABB area = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(40.0);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(CannonballEntity.class, area, ball -> ball.getOwner() == player).isEmpty(), "the ball flies");
        helper.succeedWhen(() -> {
            List<CannonballEntity> balls = helper.getLevel().getEntitiesOfClass(CannonballEntity.class, area, ball -> ball.getOwner() == player);
            helper.assertTrue(balls.isEmpty(), "the ball has landed; still flying: " + balls.stream().map(ball -> ball.position() + " v=" + ball.getDeltaMovement() + " age=" + ball.tickCount).toList() + " boat at " + boat.position());
            for (int x = 0; x < 30; x++) {
                for (int z = 0; z < 30; z++) {
                    BlockPos pos = new BlockPos(x, 0, z);
                    helper.assertBlock(pos, block -> block == Blocks.DIRT, block -> net.minecraft.network.chat.Component.literal("the floor is intact at " + pos));
                }
            }
        });
    }
}
