package com.smartvillager.progress.mixin;

import com.smartvillager.progress.happiness.HappinessTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Read-only: observes the moment two villagers actually exchange gossip - injected right at the
 * transfer inside Villager.gossip, i.e. after Vanilla's own once-a-minute cooldown check - to count
 * villager contact for happiness. Nothing about gossip itself changes. There is no NeoForge event
 * for this moment, and polling can't catch it (the INTERACTION_TARGET memory lives only a few ticks).
 */
@Mixin(Villager.class)
public abstract class VillagerGossipMixin {

    @Inject(
            method = "gossip",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/gossip/GossipContainer;transferFrom(Lnet/minecraft/world/entity/ai/gossip/GossipContainer;Lnet/minecraft/util/RandomSource;I)V")
    )
    private void smartvillager_progress$recordContact(ServerLevel level, Villager target, long timestamp, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        HappinessTracker.recordContact(level, self, target);
        HappinessTracker.recordContact(level, target, self);
    }
}
