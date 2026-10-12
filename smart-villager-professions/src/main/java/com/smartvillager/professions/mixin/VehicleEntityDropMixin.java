package com.smartvillager.professions.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.smartvillager.professions.boat.BoatDrops;
import com.smartvillager.professions.boat.DecorPerch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two spots of Vanilla's vehicle handling, for boats only (Obsidian Fisherman.md, 2026-10-12): a destroyed boat drops its item and then everything built
 * into it (BoatDrops) - the one drop inside VehicleEntity.destroy gets that added; and a boat that takes damage throws the animals out of its crow's nest and off its stern.
 * Every other vehicle and every plain boat is untouched.
 */
@Mixin(VehicleEntity.class)
public abstract class VehicleEntityDropMixin {

    @WrapOperation(
            method = "destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/Item;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;")
    )
    private ItemEntity smartvillager_professions$dropWithParts(VehicleEntity vehicle, ServerLevel level, ItemStack stack, Operation<ItemEntity> original) {
        ItemEntity dropped = original.call(vehicle, level, stack);
        if (vehicle instanceof AbstractBoat boat) {
            BoatDrops.dropAll(boat, level);
        }
        return dropped;
    }

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void smartvillager_professions$parrotFlees(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractBoat boat && cir.getReturnValueZ()) {
            DecorPerch.eject(boat);
        }
    }
}
