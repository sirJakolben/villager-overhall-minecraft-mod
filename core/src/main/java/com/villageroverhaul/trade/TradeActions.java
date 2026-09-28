package com.villageroverhaul.trade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Executing one offer without the trade screen (/vo trade): the items move here, then the offer's section books
 * it exactly as after a real trade (SectionLogic.onUse - stock for a trade, rotation for a quest). Real trades go
 * through Vanilla's result slot instead and are booked by TradeEvents.
 */
public final class TradeActions {

    public enum Result {
        SUCCESS, NOT_OFFERED, NO_USES_LEFT, CANNOT_AFFORD
    }

    private TradeActions() {
    }

    public static Result execute(Villager villager, Player player, Identifier entryId) {
        Optional<VillagerOffers.Source> source = VillagerOffers.sourceOf(villager, entryId);
        if (source.isEmpty()) {
            return Result.NOT_OFFERED;
        }
        ResolvedExchange exchange = source.get().offer().exchange();
        if (exchange.usesRemaining() <= 0) {
            return Result.NO_USES_LEFT;
        }
        if (!ExchangeExecutor.canAfford(player, exchange.input(), exchange.secondInput())) {
            return Result.CANNOT_AFFORD;
        }
        ExchangeExecutor.execute(player, exchange.input(), exchange.secondInput(), exchange.output());
        source.get().section().logic().onUse(villager, source.get().section(), source.get().offer());
        return Result.SUCCESS;
    }
}
