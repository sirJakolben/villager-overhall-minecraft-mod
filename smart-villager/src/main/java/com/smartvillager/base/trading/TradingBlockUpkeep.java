package com.smartvillager.base.trading;

import com.smartvillager.base.SmartVillagerMod;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Optional;

/**
 * A Trading Block that was broken (or replaced), or that ended up more than 48 blocks from all of the villager's workplaces (e.g. after it
 * moved to a new workplace), no longer calls its villager. With no workplace at all for the moment, the block is kept. Checked every
 * CHECK_INTERVAL ticks per villager (staggered by its tick count) - one block state read and a distance check.
 */
@EventBusSubscriber(modid = SmartVillagerMod.MODID)
public final class TradingBlockUpkeep {

    private static final int CHECK_INTERVAL = 100;

    private TradingBlockUpkeep() {
    }

    @SubscribeEvent
    static void onVillagerTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager) || !(villager.level() instanceof ServerLevel level)
                || (villager.tickCount + villager.getId()) % CHECK_INTERVAL != 0) {
            return;
        }
        Optional<GlobalPos> block = TradingBlockLinks.get(villager).filter(pos -> pos.dimension() == level.dimension());
        if (block.isEmpty()) {
            return;
        }
        boolean broken = level.isLoaded(block.get().pos()) && !level.getBlockState(block.get().pos()).is(TradingBlocks.TRADING_BLOCK.get());
        boolean tooFar = !TradingBlockCall.withinReach(villager, block.get().pos()).orElse(true);
        if (broken || tooFar) {
            TradingBlockLinks.set(villager, Optional.empty());
        }
    }
}
