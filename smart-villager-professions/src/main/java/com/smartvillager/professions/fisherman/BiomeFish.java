package com.smartvillager.professions.fisherman;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smartvillager.professions.ProfessionsRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;

/**
 * One datapack file in data/smartvillager_professions/smartvillager_professions/biome_fish/ (Obsidian Fisherman.md, "Besondere Fische", 2026-09-30): the fish that
 * bites only in these biomes - one per biome group. Both the player's fishing rod (BiomeFishModifier) and the Fisherman's fish basket
 * (passive/FishingWork) read the same files, so a group's fish is named in one place. The biome lists must not overlap - the first
 * match wins, and the registry's order is no design decision.
 */
public record BiomeFish(HolderSet<Biome> biomes, Item item) {

    public static final Codec<BiomeFish> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(BiomeFish::biomes),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(BiomeFish::item)
    ).apply(instance, BiomeFish::new));

    /** The fish of the biome group at this spot - none in plains, forest, rivers, ... */
    public static Optional<BiomeFish> at(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        for (BiomeFish fish : level.registryAccess().lookupOrThrow(ProfessionsRegistries.BIOME_FISH)) {
            if (fish.biomes().contains(biome)) {
                return Optional.of(fish);
            }
        }
        return Optional.empty();
    }
}
