package com.smartvillager.professions.veteran.test;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.veteran.MobWeapons;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * Gametests of the mob weapons (headless: ./gradlew :smart-villager-professions:runGameTestServer): a sword a zombie
 * picked up from the ground stays a normal sword when the zombie dies.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class MobWeaponGameTests {

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, SmartVillagerProfessionsMod.MODID);
    private static final int SIZE = 12;

    static {
        FUNCTIONS.register("mob_weapon_pickup_stays_plain", () -> MobWeaponGameTests::pickupStaysPlain);
    }

    private MobWeaponGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void onRegisterGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("mob_weapon_default"));
        event.registerTest(id("mob_weapon_pickup_stays_plain"), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, id("mob_weapon_pickup_stays_plain")),
                new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1, false, 8)));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, path);
    }

    /** A zombie picks up a plain iron sword thrown to it; a player kills it; the dropped sword carries no mob mark. */
    private static void pickupStaysPlain(GameTestHelper helper) {
        BlockPos low = helper.absolutePos(new BlockPos(-2, 0, -2));
        BlockPos high = helper.absolutePos(new BlockPos(SIZE + 2, 0, SIZE + 2));
        for (int chunkX = Math.min(low.getX(), high.getX()) >> 4; chunkX <= Math.max(low.getX(), high.getX()) >> 4; chunkX++) {
            for (int chunkZ = Math.min(low.getZ(), high.getZ()) >> 4; chunkZ <= Math.max(low.getZ(), high.getZ()) >> 4; chunkZ++) {
                helper.getLevel().setChunkForced(chunkX, chunkZ, true);
            }
        }
        for (int x = -1; x <= SIZE; x++) {
            for (int z = -1; z <= SIZE; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);
                for (int y = 1; y <= 4; y++) {
                    helper.setBlock(x, y, z, x < 0 || z < 0 || x >= SIZE || z >= SIZE ? Blocks.OBSIDIAN : Blocks.AIR);
                }
            }
        }
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 6, 1, 6);
        zombie.setPersistenceRequired();
        zombie.setCanPickUpLoot(true);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        zombie.setNoAi(false);
        helper.spawnItem(Items.IRON_SWORD, 6.5F, 1.0F, 6.5F);
        AABB around = new AABB(helper.absolutePos(new BlockPos(6, 1, 6))).inflate(4.0);
        boolean[] killed = {false};
        helper.onEachTick(() -> {
            if (!killed[0] && zombie.getMainHandItem().is(Items.IRON_SWORD)) {
                helper.assertTrue(MobWeapons.mobOf(zombie.getMainHandItem()).isEmpty(), "held sword is unmarked: " + zombie.getMainHandItem());
                killed[0] = true;
                @SuppressWarnings("removal") net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
                // a chunk unload and reload: the zombie is saved, loaded again and joins the world a second time
                net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                        net.minecraft.util.ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
                zombie.save(output);
                zombie.discard();
                net.minecraft.world.entity.Entity reloaded = EntityType.loadEntityRecursive(output.buildResult(), helper.getLevel(),
                        net.minecraft.world.entity.EntitySpawnReason.LOAD, net.minecraft.world.entity.EntityProcessor.NOP);
                helper.getLevel().addFreshEntity(reloaded);
                helper.assertTrue(MobWeapons.mobOf(((Zombie) reloaded).getMainHandItem()).isEmpty(), "reloaded sword is unmarked: " + ((Zombie) reloaded).getMainHandItem());
                ((Zombie) reloaded).hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1000.0F);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(killed[0], "the zombie picked up the sword; hand=" + zombie.getMainHandItem() + " pickup=" + zombie.canPickUpLoot() + " pos=" + zombie.blockPosition() + " alive=" + zombie.isAlive()
                    + " items=" + helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream().map(i -> i.getItem() + "@" + i.blockPosition()).toList());
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, drop -> drop.getItem().is(Items.IRON_SWORD));
            helper.assertTrue(drops.size() == 1, "the sword dropped, items: " + drops.size());
            helper.assertTrue(MobWeapons.mobOf(drops.get(0).getItem()).isEmpty(), "dropped sword is plain: " + drops.get(0).getItem());
        });
    }
}
