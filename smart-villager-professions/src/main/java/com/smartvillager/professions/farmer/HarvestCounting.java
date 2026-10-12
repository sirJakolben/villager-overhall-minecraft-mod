package com.smartvillager.professions.farmer;

import com.smartvillager.progress.state.VillagerState;
import com.smartvillager.progress.state.VillagerStateAccess;
import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.passive.HarvestWork;
import com.smartvillager.professions.passive.PassiveCounters;
import com.smartvillager.professions.passive.PassiveLogic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Counts the Farmer's harvests for his passive (passive/HarvestWork): every crop he really breaks is one unit on his load. Vanilla's HarvestFarmland
 * breaks a mature crop with Level.destroyBlock, naming the villager as the breaker - NeoForge's BlockDropsEvent reports exactly that, so no mixin
 * is needed. Only a Farmer who owns a harvest basket counts, and only while his load has room (his passive rank's capacity); the crop itself
 * drops and is picked up as in Vanilla either way.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class HarvestCounting {

    private HarvestCounting() {
    }

    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Villager villager) || !villager.getVillagerData().profession().is(VillagerProfession.FARMER)) {
            return;
        }
        Identifier crop = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (!HarvestWork.counts(crop)) {
            return;
        }
        VillagerState state = VillagerStateAccess.of(villager).getState();
        PassiveCounters counters = PassiveCounters.of(villager);
        if (PassiveLogic.station(state).isEmpty() || counters.total() >= PassiveCounters.capacity(PassiveLogic.rank(state))) {
            return;
        }
        PassiveCounters.set(villager, counters.withGained(crop, villager.level().getGameTime()));
        PassiveLogic.syncLoadMeter(villager);
    }
}
