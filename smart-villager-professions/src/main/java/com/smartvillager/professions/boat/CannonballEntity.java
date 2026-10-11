package com.smartvillager.professions.boat;

import com.smartvillager.professions.fisherman.FishermanItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** What the boat cannon fires: flies like a thrown item, and wherever it lands it explodes like TNT at 60 % size without breaking a block (Cannon). */
public class CannonballEntity extends ThrowableItemProjectile {

    public CannonballEntity(EntityType<? extends CannonballEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return FishermanItems.CANNONBALL.get();
    }

    @Override
    protected double getDefaultGravity() {
        return Cannon.GRAVITY;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), Cannon.EXPLOSION_RADIUS, false, Level.ExplosionInteraction.NONE);
            this.discard();
        }
    }
}
