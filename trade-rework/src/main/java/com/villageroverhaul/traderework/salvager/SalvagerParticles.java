package com.villageroverhaul.traderework.salvager;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SalvagerParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, TradeReworkMod.MODID);

    /** Lava bubble popping from the smelting station's molten metal (client/render/MoltenBubbleParticle). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MOLTEN_BUBBLE =
            PARTICLE_TYPES.register("molten_bubble", () -> new SimpleParticleType(false));

    private SalvagerParticles() {
    }
}
