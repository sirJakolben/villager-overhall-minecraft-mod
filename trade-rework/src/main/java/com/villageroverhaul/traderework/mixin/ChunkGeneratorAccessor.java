package com.villageroverhaul.traderework.mixin;

import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.function.Supplier;

/**
 * Pure access, no behavior change: the feature order per decoration step, which decides each feature's
 * random seed in a chunk (ChunkGenerator.applyBiomeDecoration). explore/FeatureLocator needs the same
 * index to predict where a feature like the amethyst geode lands.
 */
@Mixin(ChunkGenerator.class)
public interface ChunkGeneratorAccessor {

    @Accessor("featuresPerStep")
    Supplier<List<FeatureSorter.StepFeatureData>> villageroverhaul$featuresPerStep();
}
