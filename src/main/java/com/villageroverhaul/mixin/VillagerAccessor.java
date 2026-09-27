package com.villageroverhaul.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Pure access, no behavior change: lets VillagerOffers.refresh re-run Vanilla's own private
 * updateSpecialPrices (Hero of the Village + reputation) after rebuilding the offer list mid-session,
 * instead of duplicating that discount formula.
 */
@Mixin(Villager.class)
public interface VillagerAccessor {

    @Invoker("updateSpecialPrices")
    void villageroverhaul$updateSpecialPrices(Player player);
}
