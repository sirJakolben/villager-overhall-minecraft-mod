package com.smartvillager.professions.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.smartvillager.professions.boat.BoatUpgrades;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A destroyed boat drops its item - for an upgraded boat, with the cannon and the mast (and its banner) on it, so the upgrades never get lost
 * (Obsidian Fisherman.md, 2026-10-12). Only the one drop inside VehicleEntity.destroy changes; every other vehicle and every plain boat drops as before.
 */
@Mixin(VehicleEntity.class)
public abstract class VehicleEntityDropMixin {

    @WrapOperation(
            method = "destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/Item;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;")
    )
    private ItemEntity smartvillager_professions$dropWithUpgrades(VehicleEntity vehicle, ServerLevel level, ItemStack stack, Operation<ItemEntity> original) {
        if (vehicle instanceof AbstractBoat boat) {
            BoatUpgrades.applyToItem(boat, stack);
        }
        return original.call(vehicle, level, stack);
    }
}
