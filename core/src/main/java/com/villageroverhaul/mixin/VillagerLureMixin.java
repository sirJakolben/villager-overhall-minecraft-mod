package com.villageroverhaul.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villageroverhaul.interaction.EmeraldLure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Emerald lure: "forgets everything except sleep" means the villager's whole brain pauses while
 * a player lures it (EmeraldLure steers instead). Only the one brain.tick call in customServerAiStep is
 * wrapped - the rest of that method (merchant level-up timer, trade reputation, raid check) stays Vanilla.
 */
@Mixin(Villager.class)
public abstract class VillagerLureMixin {

    @WrapOperation(
            method = "customServerAiStep",
            require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/Brain;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V")
    )
    private void villageroverhaul$pauseBrainWhileLured(Brain<?> brain, ServerLevel level, LivingEntity body, Operation<Void> original) {
        if (!EmeraldLure.steer(level, (Villager) (Object) this)) {
            original.call(brain, level, body);
        }
    }
}
