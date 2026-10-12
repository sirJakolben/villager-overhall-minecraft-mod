package com.villageroverhaul.mixin;

import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.work.RestockService;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The villager's own work sound (a Mason's stonecutter sound, a Librarian's page sound) only while it
 * actually fills the meter of a list section at the station it works at (2026-09-26) - no busy noise while
 * all it can do is stand there with a full meter. At a station where only a badge section works (the Trade
 * Rework's passive stations) it stays silent: there the station makes its own sounds. See
 * RestockService.makesWorkSound.
 * One narrow cancel at the head of Villager.playWorkSound: both callers - Vanilla's WorkAtPoi at the job
 * site and our WorkAtStation at an away station - go through it, and either one only runs at the station
 * of the villager's focus. Only on the server, where the sound is made and the villager state is
 * authoritative.
 */
@Mixin(Villager.class)
public abstract class VillagerWorkSoundMixin {

    @Inject(method = "playWorkSound", at = @At("HEAD"), cancellable = true)
    private void villageroverhaul$onlyWhileFilling(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (!self.level().isClientSide() && !RestockService.makesWorkSound(self, VillagerStateAccess.of(self).getState())) {
            ci.cancel();
        }
    }
}
