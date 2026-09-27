package com.villageroverhaul.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/**
 * A passive has no modifier-expressible number - it IS a behavior, so custom_effect is mandatory
 * here. baseValue/maxValue scale linearly with the passive group's rank; what the value actually
 * means (e.g. enchant levels per day for the Librarian) is up to the custom_effect handler, Block G's job.
 */
public record PassiveAbilityDefinition(VillagerProfession profession, Identifier customEffect, double baseValue, double maxValue) {

    public static final Codec<PassiveAbilityDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.VILLAGER_PROFESSION.byNameCodec().fieldOf("profession").forGetter(PassiveAbilityDefinition::profession),
            Identifier.CODEC.fieldOf("custom_effect").forGetter(PassiveAbilityDefinition::customEffect),
            Codec.DOUBLE.fieldOf("base_value").forGetter(PassiveAbilityDefinition::baseValue),
            Codec.DOUBLE.fieldOf("max_value").forGetter(PassiveAbilityDefinition::maxValue)
    ).apply(instance, PassiveAbilityDefinition::new));
}
