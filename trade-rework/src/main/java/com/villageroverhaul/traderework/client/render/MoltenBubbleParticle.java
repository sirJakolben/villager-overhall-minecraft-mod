package com.villageroverhaul.traderework.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.LavaParticle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * The smelting station's lava bubble (SalvagerParticles.MOLTEN_BUBBLE): Vanilla's lava particle - same sprite,
 * glow, shrinking and smoke trail - but it stays around the crucible and never rolls the biggest sizes.
 */
public class MoltenBubbleParticle extends LavaParticle {

    /** Share of Vanilla's flight distance (width and height alike). */
    public static final float FLIGHT_SHARE = 0.4F;
    /** Largest size factor on the base size - Vanilla lava rolls 0.2 to 2.2. */
    public static final float MAX_SIZE_FACTOR = 1.6F;
    private static final float MIN_SIZE_FACTOR = 0.2F;

    public MoltenBubbleParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        // Under gravity both height and width grow with the square of the start speed, so the square root of the
        // share keeps the arc's shape and scales it as a whole.
        float speed = (float) Math.sqrt(FLIGHT_SHARE);
        xd *= speed;
        yd *= speed;
        zd *= speed;
        // Rolled again like Vanilla (SingleQuadParticle base size x LavaParticle factor), only with a lower top.
        float baseSize = 0.1F * (random.nextFloat() * 0.5F + 0.5F) * 2.0F;
        quadSize = baseSize * (MIN_SIZE_FACTOR + random.nextFloat() * (MAX_SIZE_FACTOR - MIN_SIZE_FACTOR));
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        return (options, level, x, y, z, xAux, yAux, zAux, random) -> new MoltenBubbleParticle(level, x, y, z, sprites.get(random));
    }
}
