package com.villageroverhaul.librarian;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Vanilla's ThrownExperienceBottle, dropping villager XP orbs (a fixed amount) in a turquoise splash. */
public class ThrownVillagerExperienceBottle extends ThrowableItemProjectile {

    /** Splash particle color (level event 2002, same one Vanilla's bottle uses) - turquoise to match the orbs. */
    private static final int SPLASH_COLOR = 0xFF1FD6B0;

    public ThrownVillagerExperienceBottle(EntityType<? extends ThrownVillagerExperienceBottle> type, Level level) {
        super(type, level);
    }

    public ThrownVillagerExperienceBottle(Level level, LivingEntity thrower, ItemStack itemStack) {
        super(LibrarianEntities.VILLAGER_EXPERIENCE_BOTTLE.get(), thrower, level, itemStack);
    }

    public ThrownVillagerExperienceBottle(Level level, double x, double y, double z, ItemStack itemStack) {
        super(LibrarianEntities.VILLAGER_EXPERIENCE_BOTTLE.get(), x, y, z, level, itemStack);
    }

    @Override
    protected Item getDefaultItem() {
        return LibrarianItems.VILLAGER_EXPERIENCE_BOTTLE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.07;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level() instanceof ServerLevel level) {
            level.levelEvent(2002, this.blockPosition(), SPLASH_COLOR);
            Vec3 direction = hitResult instanceof BlockHitResult blockHit
                    ? blockHit.getDirection().getUnitVec3()
                    : this.getDeltaMovement().scale(-1.0);
            VillagerExperienceOrb.award(level, hitResult.getLocation(), direction, ExperienceBottles.villagerBottleXp());
            this.discard();
        }
    }
}
