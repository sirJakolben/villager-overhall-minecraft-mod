package com.smartvillager.professions.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.smartvillager.professions.boat.BoatUpgrades;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/**
 * Vanilla hands an item's components to the entity it spawns here (a boat placed by hand or by a dispenser, a spawn egg ...). A boat item that
 * carries a cannon or a mast gives them to the new boat entity (Obsidian Fisherman.md, 2026-10-12); every other entity is untouched.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeBoatItemMixin {

    @ModifyReturnValue(method = "appendComponentsConfig", at = @At("RETURN"))
    private static <T extends Entity> Consumer<T> smartvillager_professions$boatUpgrades(Consumer<T> config, Consumer<T> initialConfig, ItemStack itemStack) {
        return config.andThen(entity -> {
            if (entity instanceof AbstractBoat boat) {
                BoatUpgrades.applyFromItem(boat, itemStack);
            }
        });
    }
}
