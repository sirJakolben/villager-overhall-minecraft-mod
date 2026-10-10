package com.villageroverhaul.traderework.cage;

import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.cage.Cages;
import com.villageroverhaul.traderework.cage.ZombieVillagerCageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * What the golems and the cage need to know about a zombie villager and about the cages around: the state a zombie villager is in while it is held
 * (carried by a golem, or dazed after being dropped) and the search for a free cage.
 *
 * - Carried: riding an Iron Golem. The mob has no AI then (MobMixin), so it cannot attack or walk.
 * - Dazed: for a few seconds after a golem dropped it to fight - no AI either.
 * - Unreachable: a golem gave up on it, so for a while the golems treat it like any monster again (they attack it) instead of trying again.
 * - Claimed: one golem is on its way to it - no second golem takes it.
 * The state lives in memory only (an attachment, not saved, not synced).
 */
public final class ZombieCaptives {

    /** Search radius for a free cage around the golem (blocks). */
    public static final int CAGE_RANGE = 48;
    /** How far the golem looks for a zombie villager to take (blocks). */
    public static final double SCAN_RANGE = 16.0;
    /** The zombie villager stays dazed this long after being dropped (ticks; 3 to 4 seconds). */
    public static final int DAZE_MIN_TICKS = 60;
    public static final int DAZE_EXTRA_TICKS = 20;
    /** The golems leave a zombie villager they gave up on to the Vanilla attack for this long (ticks). */
    public static final int GIVE_UP_TICKS = 1200;

    public record Info(long dazedUntil, long unreachableUntil, long claimedUntil, @Nullable UUID claimedBy) {
        static final Info NONE = new Info(0L, 0L, 0L, null);
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TradeReworkMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Info>> INFO = ATTACHMENT_TYPES.register(
            "zombie_captive", () -> AttachmentType.builder(() -> Info.NONE).build());

    private ZombieCaptives() {
    }

    private static Info info(ZombieVillager zombie) {
        return zombie.hasData(INFO) ? zombie.getData(INFO) : Info.NONE;
    }

    public static boolean isCarried(Entity entity) {
        return entity.getVehicle() instanceof IronGolem;
    }

    public static boolean isDazed(ZombieVillager zombie) {
        return zombie.level().getGameTime() < info(zombie).dazedUntil();
    }

    /** No AI: carried or dazed. */
    public static boolean isHeld(ZombieVillager zombie) {
        return isCarried(zombie) || isDazed(zombie);
    }

    public static boolean isUnreachable(ZombieVillager zombie) {
        return zombie.level().getGameTime() < info(zombie).unreachableUntil();
    }

    public static boolean isClaimedByOther(ZombieVillager zombie, UUID golem) {
        Info info = info(zombie);
        return info.claimedBy() != null && !info.claimedBy().equals(golem) && zombie.level().getGameTime() < info.claimedUntil();
    }

    public static void daze(ZombieVillager zombie, int ticks) {
        Info info = info(zombie);
        zombie.setData(INFO, new Info(zombie.level().getGameTime() + ticks, info.unreachableUntil(), info.claimedUntil(), info.claimedBy()));
    }

    public static void giveUp(ZombieVillager zombie, int ticks) {
        Info info = info(zombie);
        zombie.setData(INFO, new Info(info.dazedUntil(), zombie.level().getGameTime() + ticks, info.claimedUntil(), info.claimedBy()));
    }

    public static void claim(ZombieVillager zombie, UUID golem, int ticks) {
        Info info = info(zombie);
        zombie.setData(INFO, new Info(info.dazedUntil(), info.unreachableUntil(), zombie.level().getGameTime() + ticks, golem));
    }

    public static void unclaim(ZombieVillager zombie, UUID golem) {
        Info info = info(zombie);
        if (golem.equals(info.claimedBy())) {
            zombie.setData(INFO, new Info(info.dazedUntil(), info.unreachableUntil(), 0L, null));
        }
    }

    /** The zombie villager the golem is carrying, if any. */
    public static @Nullable ZombieVillager carriedBy(IronGolem golem) {
        for (Entity passenger : golem.getPassengers()) {
            if (passenger instanceof ZombieVillager zombie) {
                return zombie;
            }
        }
        return null;
    }

    /** Is there an empty cage around the center, whoever holds it? (What spares the zombie villagers from the golem's attack.) */
    public static boolean hasEmptyCage(ServerLevel level, BlockPos center) {
        return level.getPoiManager().findClosest(holder -> holder.value() == Cages.CAGE_POI.get(), center, CAGE_RANGE, PoiManager.Occupancy.ANY).isPresent();
    }

    /** The closest cage around the center that nobody holds, from the points of interest (loaded chunks only). */
    public static Optional<BlockPos> findFreeCage(ServerLevel level, BlockPos center, UUID golem) {
        long now = level.getGameTime();
        PoiManager pois = level.getPoiManager();
        return pois.findClosest(holder -> holder.value() == Cages.CAGE_POI.get(),
                pos -> level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ZombieVillagerCageBlockEntity cage && cage.isFreeFor(golem, now),
                center, CAGE_RANGE, PoiManager.Occupancy.ANY);
    }
}
