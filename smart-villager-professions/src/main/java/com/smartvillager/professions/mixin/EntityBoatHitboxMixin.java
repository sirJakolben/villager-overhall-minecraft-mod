package com.smartvillager.professions.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.smartvillager.professions.boat.BoatHitbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The one place an entity's box is made (Entity.makeBoundingBox, used by setPos, by every movement check and by refreshDimensions): a boat with a stern gets
 * its lengthened box here (BoatHitbox). The method is Entity's own, not AbstractBoat's, so the hook sits on Entity and answers only for boats.
 */
@Mixin(Entity.class)
public abstract class EntityBoatHitboxMixin {

    @ModifyReturnValue(method = "makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"))
    private AABB smartvillager_professions$sternHitbox(AABB box, Vec3 position) {
        return (Object) this instanceof AbstractBoat boat ? BoatHitbox.of(boat, box) : box;
    }
}
