package com.villageroverhaul.world;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

/**
 * More zombie villagers to cure (2026-09-28, emerald economy): a plain zombie that spawns naturally becomes a
 * zombie villager with CHANCE. Wherever Vanilla spawns zombies - same biomes, light and cap rules - some more
 * of them are zombie villagers; husks, drowned, spawners and every other spawn reason stay untouched.
 *
 * Vanilla weighs zombie 95 : zombie villager 5 in most biomes, so 5 % of those spawns are zombie villagers;
 * with CHANCE 20 % it is ~24 %. The swap happens before the zombie's own finalizeSpawn (which is skipped), so
 * no chicken jockey or gear is rolled for a zombie that never spawns.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class ZombieVillagerSpawns {

    /** Share of naturally spawning zombies that come as zombie villagers instead (Tweak-Werte.md). */
    public static final float CHANCE = 0.2F;

    private ZombieVillagerSpawns() {
    }

    @SubscribeEvent
    static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity().getType() != EntityType.ZOMBIE || event.getSpawnType() != EntitySpawnReason.NATURAL) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        if (level.getRandom().nextFloat() >= CHANCE) {
            return;
        }
        ZombieVillager zombieVillager = EntityType.ZOMBIE_VILLAGER.create(level, EntitySpawnReason.NATURAL);
        if (zombieVillager == null) {
            return;
        }
        event.setSpawnCancelled(true);
        event.setCanceled(true);
        zombieVillager.snapTo(event.getX(), event.getY(), event.getZ(), event.getEntity().getYRot(), 0.0F);
        EventHooks.finalizeMobSpawn(zombieVillager, level, event.getDifficulty(), EntitySpawnReason.NATURAL, null);
        if (!zombieVillager.isSpawnCancelled()) {
            level.addFreshEntityWithPassengers(zombieVillager);
        }
    }
}
