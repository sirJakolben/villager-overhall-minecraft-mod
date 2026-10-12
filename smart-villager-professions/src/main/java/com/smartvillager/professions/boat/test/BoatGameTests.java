package com.smartvillager.professions.boat.test;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.boat.BoatInventory;
import com.smartvillager.professions.boat.BoatParts;
import com.smartvillager.professions.boat.BoatSlots;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Gametests of the boat parts (headless: ./gradlew :smart-villager-professions:runGameTestServer). They also prove that the boat mixins apply: building parts
 * in with Shift + right click, the seats that are left, everything dropping when the boat is destroyed, the Fisherman's payments and the mast offer's result,
 * the cannon's ammo and its explosion without block damage, and the parrot in the crow's nest.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class BoatGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SmartVillagerProfessionsMod.MODID);
    private static final List<String> NAMES = List.of("boat_build_in_and_drop", "boat_trade_payments", "boat_seats", "cannon_needs_ball", "cannon_ball_keeps_blocks", "boat_parrot_perch", "boat_pet_perch", "boat_shoulder_parrot", "boat_stern_hitbox", "boat_chest_and_banner_clicks");

    static {
        FUNCTIONS.register("boat_build_in_and_drop", () -> BoatGameTests::buildInAndDrop);
        FUNCTIONS.register("boat_trade_payments", () -> BoatGameTests::tradePayments);
        FUNCTIONS.register("boat_seats", () -> BoatGameTests::seats);
        FUNCTIONS.register("cannon_needs_ball", () -> BoatGameTests::needsBall);
        FUNCTIONS.register("cannon_ball_keeps_blocks", () -> BoatGameTests::ballKeepsBlocks);
        FUNCTIONS.register("boat_parrot_perch", () -> BoatGameTests::parrotPerch);
        FUNCTIONS.register("boat_pet_perch", () -> BoatGameTests::petPerch);
        FUNCTIONS.register("boat_shoulder_parrot", () -> BoatGameTests::shoulderParrot);
        FUNCTIONS.register("boat_stern_hitbox", () -> BoatGameTests::sternHitbox);
        FUNCTIONS.register("boat_chest_and_banner_clicks", () -> BoatGameTests::chestAndBannerClicks);
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

    private static AbstractBoat boat(GameTestHelper helper, int x, int y, int z) {
        AbstractBoat boat = (AbstractBoat) EntityType.SPRUCE_BOAT.create(helper.getLevel(), EntitySpawnReason.SPAWN_ITEM_USE);
        Vec3 position = helper.absoluteVec(new Vec3(x, y, z));
        boat.setInitialPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(boat);
        return boat;
    }

    private static ItemStack sailItem(ItemStack banner) {
        ItemStack mast = new ItemStack(FishermanItems.BOAT_SAIL.get());
        mast.set(BoatUpgrades.SAIL.get(), new BoatUpgrades.Sail(banner));
        return mast;
    }

    private static BoatState withCannon(BoatState state) {
        return state.withPart(0, BoatParts.CANNON.id());
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

    /** Shift + right click on the boat's front or rear half with a part in the hand, as the event sees it. */
    private static void click(GameTestHelper helper, ServerPlayer player, AbstractBoat boat, ItemStack part, double forward) {
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, part);
        double yaw = Math.toRadians(boat.getYRot());
        Vec3 local = new Vec3(-Math.sin(yaw) * forward, 0.3, Math.cos(yaw) * forward);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, boat, local));
    }

    /** Parts are built in by Shift + click into the slot that was clicked; the cannon only goes to the front; the boat gives everything back when destroyed. */
    private static void buildInAndDrop(GameTestHelper helper) {
        floor(helper, 12, 12, Blocks.STONE);
        AbstractBoat boat = boat(helper, 6, 1, 6);
        @SuppressWarnings("removal") ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        click(helper, player, boat, new ItemStack(FishermanItems.BOAT_CHEST.get()), -0.6);
        helper.assertTrue(BoatUpgrades.state(boat).part(1).equals(BoatParts.CHEST.id()), "a chest clicked at the rear goes into the rear slot: " + BoatUpgrades.state(boat));
        click(helper, player, boat, new ItemStack(FishermanItems.BOAT_CHEST.get()), -0.6);
        helper.assertTrue(BoatUpgrades.state(boat).part(0).equals(BoatParts.CHEST.id()), "the second chest takes the next free slot: " + BoatUpgrades.state(boat));
        click(helper, player, boat, new ItemStack(FishermanItems.BOAT_CANNON.get()), 0.2);
        helper.assertTrue(BoatUpgrades.state(boat).part(0).equals(BoatParts.CHEST.id()), "a cannon does not fit into a taken front slot");
        click(helper, player, boat, sailItem(new ItemStack(Items.RED_BANNER)), 0.2);
        helper.assertTrue(BoatUpgrades.state(boat).sailSlot() == 0 && BoatUpgrades.state(boat).banner().is(Items.RED_BANNER), "the mast went to the clicked slot with its banner");
        click(helper, player, boat, sailItem(new ItemStack(Items.WHITE_BANNER)), -0.6);
        helper.assertTrue(BoatUpgrades.state(boat).banner().is(Items.RED_BANNER), "one mast per boat");
        helper.assertTrue(BoatUpgrades.state(boat).chests() == 2, "two chests built in");
        click(helper, player, boat, new ItemStack(FishermanItems.BOAT_STERN.get()), 0.2);
        helper.assertTrue(BoatUpgrades.state(boat).stern() && BoatUpgrades.state(boat).deco(BoatParts.STERN).get().slot() == 1, "the stern always goes to the rear slot, next to the sail: " + BoatUpgrades.state(boat));
        click(helper, player, boat, new ItemStack(FishermanItems.BOAT_STERN.get()), -0.6);
        helper.assertTrue(BoatUpgrades.state(boat).decos().size() == 2, "one of each decoration");

        boat.setData(com.smartvillager.professions.boat.BoatInventory.CONTENTS, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 3))));
        boat.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 5.0F);
        AABB around = new AABB(boat.blockPosition()).inflate(4.0);
        helper.succeedWhen(() -> {
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around);
            helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(Items.SPRUCE_BOAT)).count() == 1, "the boat itself dropped");
            helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(FishermanItems.BOAT_CHEST.get())).count() == 2, "both chests dropped");
            helper.assertTrue(drops.stream().anyMatch(drop -> drop.getItem().is(Items.DIAMOND) && drop.getItem().getCount() == 3), "the chests' contents dropped");
            helper.assertTrue(drops.stream().filter(drop -> drop.getItem().is(FishermanItems.BOAT_STERN.get())).count() == 1, "the stern dropped");
            helper.assertTrue(drops.stream().anyMatch(drop -> {
                BoatUpgrades.Sail mast = drop.getItem().get(BoatUpgrades.SAIL.get());
                return drop.getItem().is(FishermanItems.BOAT_SAIL.get()) && mast != null && mast.banner().is(Items.RED_BANNER);
            }), "the mast dropped with its banner");
        });
    }

    /** The Fisherman's four offers: the mast takes any banner and returns a mast with that very banner; the rest are plain item trades. */
    private static void tradePayments(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var exchanges = registries.lookupOrThrow(ModDataPackRegistries.EXCHANGE);
        for (String name : List.of("fisherman_boat_chest", "fisherman_boat_sail", "fisherman_boat_stern", "fisherman_boat_cannon", "fisherman_cannonball")) {
            helper.assertTrue(exchanges.getValue(id(name)) != null, "the entry " + name + " loaded");
        }
        ItemExchange mastOffer = exchanges.getValue(id("fisherman_boat_sail"));
        ItemCost banner = RequiredEnchantmentCost.of(mastOffer.secondInput().get(), registries);
        helper.assertTrue(banner.test(new ItemStack(Items.RED_BANNER)) && banner.test(new ItemStack(Items.WHITE_BANNER)), "any banner pays for a mast");
        helper.assertTrue(!banner.test(new ItemStack(Items.OAK_PLANKS)), "planks do not");

        ItemStack shown = new ItemStack(FishermanItems.BOAT_SAIL.get());
        MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD), Optional.of(banner), shown, 0, 1, 0, 0.0F);
        ItemStack paid = new ItemStack(Items.RED_BANNER);
        ItemStack result = BoatUpgrades.transform(offer, new ItemStack(Items.EMERALD, 4), paid, shown);
        BoatUpgrades.Sail mast = result.get(BoatUpgrades.SAIL.get());
        helper.assertTrue(result.is(FishermanItems.BOAT_SAIL.get()) && mast != null && mast.banner().is(Items.RED_BANNER), "the mast carries the paid banner: " + result);
        ItemStack plain = new ItemStack(FishermanItems.BOAT_CANNON.get());
        helper.assertTrue(BoatUpgrades.transform(offer, new ItemStack(Items.EMERALD), new ItemStack(Items.IRON_BLOCK), plain) == plain, "other offers pass through untouched");
        helper.succeed();
    }

    /** A filled slot is a seat less; a mast takes none; a parrot may join a mast boat beyond the seats; the passenger sits at the free slot. */
    private static void seats(GameTestHelper helper) {
        floor(helper, 16, 12, Blocks.STONE);
        AbstractBoat plain = boat(helper, 3, 1, 3);
        AbstractBoat chest = boat(helper, 8, 1, 3);
        AbstractBoat full = boat(helper, 13, 1, 3);
        AbstractBoat cannon = boat(helper, 3, 1, 9);
        AbstractBoat mast = boat(helper, 8, 1, 9);
        BoatUpgrades.setState(chest, BoatUpgrades.state(chest).withPart(1, BoatParts.CHEST.id()));
        BoatUpgrades.setState(full, withCannon(BoatUpgrades.state(full)).withPart(1, BoatParts.CHEST.id()));
        BoatUpgrades.setState(cannon, withCannon(BoatUpgrades.state(cannon)));
        BoatUpgrades.setState(mast, BoatUpgrades.state(mast).withDeco(new BoatState.Deco(BoatParts.SAIL.id(), 1, sailItem(ItemStack.EMPTY))));

        Sheep[] sheep = new Sheep[9];
        int[][] at = {{3, 3}, {3, 3}, {8, 3}, {8, 3}, {13, 3}, {3, 9}, {3, 9}, {8, 9}, {8, 9}};
        for (int i = 0; i < sheep.length; i++) {
            sheep[i] = helper.spawn(EntityType.SHEEP, at[i][0], 1, at[i][1]);
        }
        helper.assertTrue(sheep[0].startRiding(plain) && sheep[1].startRiding(plain), "a plain boat takes two");
        helper.assertTrue(sheep[2].startRiding(chest) && !sheep[3].startRiding(chest), "a chest takes a seat: one passenger");
        helper.assertTrue(!sheep[4].startRiding(full), "cannon and chest fill the boat: nobody gets in");
        helper.assertTrue(sheep[5].startRiding(cannon) && !sheep[6].startRiding(cannon), "a cannon takes a seat: one passenger");
        helper.assertTrue(BoatSlots.seatOffset(cannon, sheep[5]) == -0.6, "with the cannon in front the passenger sits at the rear: " + BoatSlots.seatOffset(cannon, sheep[5]));
        helper.assertTrue(BoatSlots.seatOffset(chest, sheep[2]) == 0.2, "with a chest in the rear the passenger sits in front: " + BoatSlots.seatOffset(chest, sheep[2]));
        helper.assertTrue(Double.isNaN(BoatSlots.seatOffset(plain, sheep[0])), "a plain boat keeps Vanilla's seating");
        helper.assertTrue(sheep[7].startRiding(mast) && sheep[8].startRiding(mast), "a mast takes no seat");
        Parrot parrot = helper.spawn(EntityType.PARROT, 8, 1, 9);
        helper.assertTrue(parrot.startRiding(mast), "a parrot may perch on a mast boat that has all seats taken");
        AbstractBoat stern = boat(helper, 13, 1, 9);
        BoatUpgrades.setState(stern, BoatUpgrades.state(stern).withDeco(new BoatState.Deco(BoatParts.STERN.id(), 1, new ItemStack(FishermanItems.BOAT_STERN.get()))));
        Sheep sternSheepA = helper.spawn(EntityType.SHEEP, 13, 1, 9);
        Sheep sternSheepB = helper.spawn(EntityType.SHEEP, 13, 1, 9);
        helper.assertTrue(sternSheepA.startRiding(stern) && sternSheepB.startRiding(stern), "a stern takes no seat");
        helper.assertTrue(helper.spawn(EntityType.CAT, 13, 1, 9).startRiding(stern), "a cat sits on the deck");
        helper.assertTrue(!helper.spawn(EntityType.WOLF, 13, 1, 9).startRiding(stern), "the deck has a place for one pet only");
        helper.succeed();
    }

    /** Without a cannonball the shot only thuds; with one it fires and uses it up; then the cannon reloads. */
    private static void needsBall(GameTestHelper helper) {
        floor(helper, 12, 12, Blocks.STONE);
        AbstractBoat boat = boat(helper, 6, 1, 3);
        BoatUpgrades.setState(boat, withCannon(BoatUpgrades.state(boat)));
        @SuppressWarnings("removal") ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(boat.getX(), boat.getY(), boat.getZ());
        helper.assertTrue(player.startRiding(boat), "the player boards");
        player.getInventory().add(new ItemStack(FishermanItems.CANNONBALL.get(), 1));
        Cannon.fire(boat, 40);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(CannonballEntity.class, boat.getBoundingBox().inflate(6.0), ball -> ball.getOwner() == player).isEmpty() && player.getInventory().countItem(FishermanItems.CANNONBALL.get()) == 1, "a bar under half fires nothing and uses nothing");
        player.getInventory().clearContent();
        Cannon.fire(boat, 80);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(CannonballEntity.class, boat.getBoundingBox().inflate(6.0), ball -> ball.getOwner() == player).isEmpty(), "no ball without a cannonball");
        helper.assertTrue(BoatUpgrades.state(boat).cooldown(helper.getLevel().getGameTime()) == 0, "a thud does not start the reload");

        player.getInventory().add(new ItemStack(FishermanItems.CANNONBALL.get(), 2));
        Cannon.fire(boat, 80);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(CannonballEntity.class, boat.getBoundingBox().inflate(6.0), ball -> ball.getOwner() == player).size() == 1, "one ball in the air");
        helper.assertTrue(player.getInventory().countItem(FishermanItems.CANNONBALL.get()) == 1, "one cannonball used up");
        Cannon.fire(boat, 80);
        helper.assertTrue(player.getInventory().countItem(FishermanItems.CANNONBALL.get()) == 1, "still reloading: no second shot");
        helper.getLevel().getEntitiesOfClass(CannonballEntity.class, boat.getBoundingBox().inflate(6.0), ball -> ball.getOwner() == player).forEach(CannonballEntity::discard);
        helper.succeed();
    }

    /** The ball lands in a floor of dirt (which TNT would crater) and explodes - no hole appears (neighbouring tests may have turned some dirt into stone, never into air). */
    private static void ballKeepsBlocks(GameTestHelper helper) {
        floor(helper, 40, 40, Blocks.DIRT);
        AbstractBoat boat = boat(helper, 15, 1, 2);
        BoatUpgrades.setState(boat, withCannon(BoatUpgrades.state(boat)));
        @SuppressWarnings("removal") ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        player.setPos(boat.getX(), boat.getY(), boat.getZ());
        helper.assertTrue(player.startRiding(boat), "the player boards");
        Cannon.fire(boat, 51);
        AABB area = new AABB(helper.absolutePos(new BlockPos(0, 0, 0))).inflate(40.0);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(CannonballEntity.class, area, ball -> ball.getOwner() == player).isEmpty(), "the ball flies");
        helper.succeedWhen(() -> {
            List<CannonballEntity> balls = helper.getLevel().getEntitiesOfClass(CannonballEntity.class, area, ball -> ball.getOwner() == player);
            helper.assertTrue(balls.isEmpty(), "the ball has landed; still flying: " + balls.stream().map(ball -> ball.position() + " v=" + ball.getDeltaMovement() + " age=" + ball.tickCount).toList() + " boat at " + boat.position());
            for (int x = 0; x < 30; x++) {
                for (int z = 0; z < 30; z++) {
                    BlockPos pos = new BlockPos(x, 0, z);
                    helper.assertBlock(pos, block -> block != Blocks.AIR, block -> Component.literal("the floor is intact at " + pos));
                }
            }
        });
    }

    /** A tame dog of the player walks to the stern deck while the player sits in the boat, and is thrown off when the boat takes damage. */
    private static void petPerch(GameTestHelper helper) {
        floor(helper, 14, 14, Blocks.STONE);
        AbstractBoat boat = boat(helper, 7, 1, 7);
        BoatUpgrades.setState(boat, BoatUpgrades.state(boat).withDeco(new BoatState.Deco(BoatParts.STERN.id(), 1, new ItemStack(FishermanItems.BOAT_STERN.get()))));
        @SuppressWarnings("removal") ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(boat.getX(), boat.getY(), boat.getZ());
        helper.assertTrue(player.startRiding(boat), "the player boards");
        Wolf wolf = helper.spawn(EntityType.WOLF, 10, 1, 10);
        wolf.tame(player);
        wolf.setInSittingPose(false);
        wolf.setOrderedToSit(false);
        boolean[] thrownOff = {false};
        int[] aboardTicks = {0};
        int[] stage = {0};
        int[] waited = {0};
        helper.onEachTick(() -> {
            if (thrownOff[0]) {
                return;
            }
            if (stage[0] == 0 && wolf.getVehicle() == boat && ++aboardTicks[0] > 5) {
                helper.assertTrue(boat.getPassengers().size() == 2, "player and dog aboard");
                helper.assertTrue(wolf.isInSittingPose(), "the dog sits on the deck");
                player.stopRiding();
                stage[0] = 1;
            } else if (stage[0] == 1 && ++waited[0] > 20) {
                helper.assertTrue(wolf.getVehicle() == boat, "the dog stays on the deck when the player gets out");
                boat.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 0.1F);
                thrownOff[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(thrownOff[0], "the dog got on the stern (vehicle: " + wolf.getVehicle() + ", at " + wolf.position() + ")");
            helper.assertTrue(wolf.getVehicle() == null, "the damage threw the dog off");
        });
    }

    private static void rightClick(ServerPlayer player, AbstractBoat boat, ItemStack held, boolean shift) {
        player.setShiftKeyDown(shift);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, boat));
    }

    /** Shift + shears take the sail's banner off, Shift + banner puts one on the bare mast, Shift + anything else opens the chests (27 per chest, kept); without Shift nothing of this happens. */
    private static void chestAndBannerClicks(GameTestHelper helper) {
        floor(helper, 12, 12, Blocks.STONE);
        AbstractBoat boat = boat(helper, 6, 1, 6);
        BoatUpgrades.setState(boat, BoatUpgrades.state(boat).withPart(0, BoatParts.CHEST.id()).withPart(1, BoatParts.CHEST.id())
                .withDeco(new BoatState.Deco(BoatParts.SAIL.id(), 1, sailItem(new ItemStack(Items.RED_BANNER)))));
        @SuppressWarnings("removal") ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        ItemStack shears = new ItemStack(Items.SHEARS);
        rightClick(player, boat, shears, false);
        helper.assertTrue(BoatUpgrades.state(boat).banner().is(Items.RED_BANNER), "shears without Shift leave the banner");
        rightClick(player, boat, shears, true);
        helper.assertTrue(!BoatUpgrades.state(boat).hasBanner() && BoatUpgrades.state(boat).sail(), "Shift + shears take the banner off, the mast stays");
        helper.assertTrue(player.getInventory().countItem(Items.RED_BANNER) == 1 && shears.getDamageValue() == 1, "the banner comes back and the shears wear");

        rightClick(player, boat, new ItemStack(Items.BLUE_BANNER), false);
        helper.assertTrue(!BoatUpgrades.state(boat).hasBanner(), "a banner without Shift does nothing");
        ItemStack blue = new ItemStack(Items.BLUE_BANNER, 2);
        rightClick(player, boat, blue, true);
        helper.assertTrue(BoatUpgrades.state(boat).banner().is(Items.BLUE_BANNER) && blue.getCount() == 1, "Shift + banner puts one on the bare mast");

        rightClick(player, boat, new ItemStack(Items.GREEN_BANNER), true);
        helper.assertTrue(BoatUpgrades.state(boat).banner().is(Items.BLUE_BANNER), "a mast with a banner keeps it");
        helper.assertTrue(player.containerMenu instanceof ChestMenu menu && menu.getRowCount() == 6, "Shift + click then opens the double chest: " + player.containerMenu);
        ((ChestMenu) player.containerMenu).getContainer().setItem(0, new ItemStack(Items.DIAMOND, 5));
        player.closeContainer();
        helper.assertTrue(BoatInventory.contents(boat).getStackInSlot(0).is(Items.DIAMOND), "the chest keeps what was put in");
        rightClick(player, boat, ItemStack.EMPTY, true);
        helper.assertTrue(player.containerMenu instanceof ChestMenu reopened && reopened.getContainer().getItem(0).getCount() == 5, "and shows it when opened again");
        helper.succeed();
    }

    /** A stern makes the hitbox 50 % longer, towards the stern only (a boat facing south: the extra is on its north side). */
    private static void sternHitbox(GameTestHelper helper) {
        floor(helper, 14, 14, Blocks.STONE);
        AbstractBoat plain = boat(helper, 4, 1, 7);
        AbstractBoat stern = boat(helper, 10, 1, 7);
        BoatUpgrades.setState(stern, BoatUpgrades.state(stern).withDeco(new BoatState.Deco(BoatParts.STERN.id(), 1, new ItemStack(FishermanItems.BOAT_STERN.get()))));
        helper.succeedWhen(() -> {
            AABB plainBox = plain.getBoundingBox();
            AABB box = stern.getBoundingBox();
            helper.assertTrue(Math.abs(plainBox.getZsize() - 1.375) < 1.0E-3, "a plain boat keeps its box: " + plainBox);
            helper.assertTrue(Math.abs(box.getZsize() - 1.375 * 1.5) < 1.0E-3 && Math.abs(box.getXsize() - 1.375) < 1.0E-3, "the stern boat is 50 % longer, not wider: " + box);
            helper.assertTrue(Math.abs(box.maxZ - (stern.getZ() + 1.375 / 2.0)) < 1.0E-3 && Math.abs(box.minZ - (stern.getZ() - 1.375)) < 1.0E-3, "all of the extra is at the stern (yaw " + stern.getYRot() + "): " + box + " at z " + stern.getZ());
        });
    }

    /** A parrot on the player's shoulder is taken off it and put on the crow's nest when the player sits in a boat with a sail. */
    private static void shoulderParrot(GameTestHelper helper) {
        floor(helper, 14, 14, Blocks.STONE);
        AbstractBoat boat = boat(helper, 7, 1, 7);
        BoatUpgrades.setState(boat, BoatUpgrades.state(boat).withDeco(new BoatState.Deco(BoatParts.SAIL.id(), 1, sailItem(new ItemStack(Items.WHITE_BANNER)))));
        @SuppressWarnings("removal") ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(boat.getX(), boat.getY(), boat.getZ());
        Parrot parrot = helper.spawn(EntityType.PARROT, 7, 1, 7);
        parrot.tame(player);
        player.setOnGround(true);
        helper.assertTrue(parrot.setEntityOnShoulder(player), "the parrot sits on the shoulder");
        helper.assertTrue(!player.getShoulderEntityLeft().isEmpty() || !player.getShoulderEntityRight().isEmpty(), "a shoulder is taken");
        helper.assertTrue(player.startRiding(boat), "the player boards");
        helper.succeedWhen(() -> {
            helper.assertTrue(boat.getPassengers().stream().anyMatch(passenger -> passenger instanceof Parrot), "the parrot is on the nest");
            helper.assertTrue(player.getShoulderEntityLeft().isEmpty() && player.getShoulderEntityRight().isEmpty(), "both shoulders are free again");
        });
    }

    /** A tame parrot of the player flies to the crow's nest while the player sits in a boat with a mast, and is thrown out when the boat takes damage. */
    private static void parrotPerch(GameTestHelper helper) {
        floor(helper, 14, 14, Blocks.STONE);
        AbstractBoat boat = boat(helper, 7, 1, 7);
        BoatUpgrades.setState(boat, BoatUpgrades.state(boat).withDeco(new BoatState.Deco(BoatParts.SAIL.id(), 1, sailItem(new ItemStack(Items.WHITE_BANNER)))));
        @SuppressWarnings("removal") ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(boat.getX(), boat.getY(), boat.getZ());
        helper.assertTrue(player.startRiding(boat), "the player boards");
        Parrot parrot = helper.spawn(EntityType.PARROT, 9, 3, 9);
        parrot.tame(player);
        parrot.setInSittingPose(false);
        parrot.setOrderedToSit(false);
        boolean[] thrownOut = {false};
        helper.onEachTick(() -> {
            if (!thrownOut[0] && parrot.getVehicle() == boat) {
                helper.assertTrue(boat.getPassengers().size() == 2, "player and parrot aboard");
                boat.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 0.1F);
                thrownOut[0] = true;
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(thrownOut[0], "the parrot perched on the mast (vehicle: " + parrot.getVehicle() + ", at " + parrot.position() + ")");
            helper.assertTrue(parrot.getVehicle() == null, "the damage threw the parrot out");
        });
    }
}
