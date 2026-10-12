package com.smartvillager.professions.boat;

import com.smartvillager.professions.fisherman.FishermanEntities;
import com.smartvillager.professions.fisherman.FishermanItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The cannon boat's numbers and the shot itself (Obsidian Fisherman.md, 2026-10-12; numbers in Tweak-Werte.md). The charge bar is Vanilla's
 * horse jump bar (the mixin makes cannon boats a PlayerRideableJumping); the client sends the charge when Space is released, and the
 * server fires here. The muzzle position and direction come from the Blockbench model (assets/Fisherman/boat.bbmodel): the barrel's front
 * end, 20 degrees up.
 */
public final class Cannon {

    /** Top speed of a boat with a cannon: 40 % less (the turn rate is not touched). */
    public static final float SPEED_FACTOR = 0.6F;
    /** The jump bar charges at this share of the horse's speed (half as fast). */
    public static final float CHARGE_SPEED = 0.5F;
    /** A TNT explosion is 4.0; the cannonball's is 60 % of it, and never destroys blocks. */
    public static final float EXPLOSION_RADIUS = 4.0F * 0.6F;
    /** Ticks between two shots. */
    public static final int COOLDOWN_TICKS = 30;
    /** How long soot rises from the muzzle after a shot, and how often it puffs. */
    public static final int SOOT_TICKS = 40;
    public static final int SOOT_INTERVAL = 2;
    /** Where the cannon sits in the model (the front slot of a Vanilla boat), in blocks along the boat: another boat shifts the cannon with its front slot. */
    public static final double MODEL_SLOT_OFFSET = 0.2;
    /** Launch speed in blocks per tick: the least a released bar gives, and what a full bar adds. */
    public static final float MIN_SPEED = 0.5F;
    public static final float CHARGE_SPEED_GAIN = 2.0F;
    /** The bar must be filled over this many percent when released, or nothing is fired (and nothing is used up). */
    public static final int MIN_CHARGE = 10;
    public static final double GRAVITY = 0.04;
    /** The ball leaves 5 degrees above the horizon (the model's barrel is tilted more, for the look). */
    private static final double PITCH = Math.toRadians(5.0);
    /** Muzzle centre in front of and above the boat's middle, in blocks (model: 20.8 px forward, 14.6 px above the hitbox bottom). */
    private static final double MUZZLE_FORWARD = 20.79 / 16.0;
    private static final double MUZZLE_UP = 14.55 / 16.0;

    private Cannon() {
    }

    /** A unit vector along the boat's bow. */
    private static Vec3 forward(AbstractBoat boat) {
        double yaw = Math.toRadians(boat.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    public static Vec3 muzzle(AbstractBoat boat) {
        double ahead = MUZZLE_FORWARD + BoatSlots.offset(boat, 0) - MODEL_SLOT_OFFSET;
        return boat.position().add(forward(boat).scale(ahead)).add(0.0, MUZZLE_UP, 0.0);
    }

    /** The ball's direction: forward, a little above the horizon. */
    public static Vec3 direction(AbstractBoat boat) {
        return forward(boat).scale(Math.cos(PITCH)).add(0.0, Math.sin(PITCH), 0.0);
    }

    /** The shot: charge is 1..100 (the bar's fill in percent) and must be over MIN_CHARGE. Without a cannonball the cannon only thuds. */
    public static void fire(AbstractBoat boat, int charge) {
        if (!(boat.level() instanceof ServerLevel level) || charge <= MIN_CHARGE) {
            return;
        }
        BoatState state = BoatUpgrades.state(boat);
        if (!state.cannon() || state.cooldown(level.getGameTime()) > 0) {
            return;
        }
        LivingEntity rider = boat.getControllingPassenger();
        Vec3 muzzle = muzzle(boat);
        if (!consumeBall(rider)) {
            level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.IRON_GOLEM_STEP, SoundSource.NEUTRAL, 1.0F, 0.7F);
            return;
        }
        BoatUpgrades.setState(boat, state.withShot(level.getGameTime()));

        float power = Math.clamp(charge / 100.0F, 0.0F, 1.0F);
        CannonballEntity ball = new CannonballEntity(FishermanEntities.CANNONBALL.get(), level);
        ball.setItem(new ItemStack(FishermanItems.CANNONBALL.get()));
        ball.setOwner(rider);
        ball.setPos(muzzle);
        Vec3 direction = direction(boat);
        ball.shoot(direction.x, direction.y, direction.z, MIN_SPEED + CHARGE_SPEED_GAIN * power, 0.0F);
        level.addFreshEntity(ball);

        level.playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.0F, 1.4F);
        level.sendParticles(ParticleTypes.EXPLOSION, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 8, 0.1, 0.1, 0.1, 0.05);
    }

    /** One cannonball out of the rider's inventory (creative players need none). */
    private static boolean consumeBall(LivingEntity rider) {
        if (!(rider instanceof Player player)) {
            return false;
        }
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(FishermanItems.CANNONBALL.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Server tick of a cannon boat: soot rises from the muzzle for two seconds after a shot. */
    public static void tickSoot(AbstractBoat boat, BoatState state) {
        if (!(boat.level() instanceof ServerLevel level)) {
            return;
        }
        long since = level.getGameTime() - state.lastShot();
        if (since > 0 && since <= SOOT_TICKS && since % SOOT_INTERVAL == 0) {
            Vec3 muzzle = muzzle(boat);
            level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 0, 0.0, 1.0, 0.0, 0.04);
        }
    }
}
