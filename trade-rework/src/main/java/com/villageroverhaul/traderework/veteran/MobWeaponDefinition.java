package com.villageroverhaul.traderework.veteran;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/**
 * One datapack file in data/villageroverhaul/villageroverhaul/mob_weapon/: a hostile mob whose held weapon
 * becomes a mob weapon (Veteran.md, 2026-09-26). effect (optional) is what the mob itself gives you on
 * a hit, with its Vanilla duration in ticks - the weapon passes it on at MobWeapons.EFFECT_CHANCE with
 * MobWeapons.EFFECT_DURATION_FACTOR of that duration. cooldown_reduction (optional, e.g. 0.2 = 20 % shorter
 * attack cooldown) speeds up the weapon's attacks (MobWeaponEvents.onItemAttributeModifiers). charge_reduction
 * (optional, e.g. 0.2 = 20 % shorter, 2026-09-27) shortens a crossbow's draw (mixin/CrossbowChargeMixin).
 */
public record MobWeaponDefinition(EntityType<?> entity, Optional<Effect> effect, float cooldownReduction, float chargeReduction) {

    public record Effect(Holder<MobEffect> effect, int duration, int amplifier) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("id").forGetter(Effect::effect),
                Codec.INT.fieldOf("duration").forGetter(Effect::duration),
                Codec.INT.optionalFieldOf("amplifier", 0).forGetter(Effect::amplifier)
        ).apply(instance, Effect::new));
    }

    public static final Codec<MobWeaponDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(MobWeaponDefinition::entity),
            Effect.CODEC.optionalFieldOf("effect").forGetter(MobWeaponDefinition::effect),
            Codec.FLOAT.optionalFieldOf("cooldown_reduction", 0.0F).forGetter(MobWeaponDefinition::cooldownReduction),
            Codec.FLOAT.optionalFieldOf("charge_reduction", 0.0F).forGetter(MobWeaponDefinition::chargeReduction)
    ).apply(instance, MobWeaponDefinition::new));
}
