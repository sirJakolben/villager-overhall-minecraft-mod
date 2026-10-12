package com.villageroverhaul.traderework.explore;

import com.villageroverhaul.traderework.mixin.ChunkGeneratorAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Finds where a placed feature (the amethyst geode, Mason's geode map, 2026-09-27) generates - from the
 * world seed alone, without loading or generating a single chunk. Geodes are no structure, so /locate and
 * Vanilla's explorer maps can't find them.
 *
 * It replays exactly what ChunkGenerator.applyBiomeDecoration does per chunk: the decoration seed from the
 * world seed and the chunk corner, the feature seed from the feature's index in its step, then the placed
 * feature's modifiers in order (rarity, square, height ...). The biome filter would ask the level for the
 * biome, which loads the chunk - it is answered from the biome source instead. The one thing it can't
 * check is the feature's own last-moment refusal (a geode next to too much air or water is skipped), so
 * now and then the mark sits on a spot where nothing generated.
 *
 * For features that refuse most of their tries - dungeons (2026-09-27, Veteran's dungeon map): of ~14 tries
 * per chunk only the few with a fitting cave pocket become a dungeon - a verify block can be given: each
 * candidate, closest first, is then checked in the real world (the dungeon's spawner sits exactly on the
 * predicted spot). That loads the candidate's chunk - generating it if nobody has been there - so it is
 * capped at MAX_VERIFY_CHECKS candidates.
 */
public final class FeatureLocator {

    /** Candidates checked in the world at most before a verified search gives up. */
    private static final int MAX_VERIFY_CHECKS = 64;

    private record Placed(PlacedFeature feature, int step, int index) {
    }

    private FeatureLocator() {
    }

    public static Optional<BlockPos> findNearest(ServerLevel level, List<ResourceKey<PlacedFeature>> keys, BlockPos origin, int radiusChunks,
                                                 Optional<Block> verifyBlock) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        List<FeatureSorter.StepFeatureData> steps = ((ChunkGeneratorAccessor) generator).villageroverhaul$featuresPerStep().get();
        List<Placed> placed = new ArrayList<>();
        for (ResourceKey<PlacedFeature> key : keys) {
            level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(key)
                    .flatMap(holder -> find(steps, holder.value()))
                    .ifPresent(placed::add);
        }
        if (placed.isEmpty()) {
            return Optional.empty(); // none of the features generates in this dimension
        }
        Function<ChunkPos, Stream<BlockPos>> candidates = chunk -> placed.stream()
                .flatMap(p -> positionsIn(level, generator, p.feature(), p.step(), p.index(), chunk));
        if (verifyBlock.isEmpty()) {
            return ChunkSearch.nearest(origin, radiusChunks, candidates);
        }
        Block block = verifyBlock.get();
        return ChunkSearch.nearest(origin, radiusChunks, candidates, pos -> level.getBlockState(pos).is(block), MAX_VERIFY_CHECKS);
    }

    /** Step and index of the feature in the generator's decoration order - they decide its random seed. */
    private static Optional<Placed> find(List<FeatureSorter.StepFeatureData> steps, PlacedFeature feature) {
        for (int step = 0; step < steps.size(); step++) {
            List<PlacedFeature> features = steps.get(step).features();
            for (int index = 0; index < features.size(); index++) {
                if (features.get(index) == feature) {
                    return Optional.of(new Placed(feature, step, index));
                }
            }
        }
        return Optional.empty();
    }

    private static Stream<BlockPos> positionsIn(ServerLevel level, ChunkGenerator generator, PlacedFeature feature, int step, int index, ChunkPos chunk) {
        BlockPos chunkOrigin = new BlockPos(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ());
        WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(0L));
        long decorationSeed = random.setDecorationSeed(level.getSeed(), chunkOrigin.getX(), chunkOrigin.getZ());
        random.setFeatureSeed(decorationSeed, index, step);

        PlacementContext context = new PlacementContext(level, generator, Optional.of(feature));
        RandomState randomState = level.getChunkSource().randomState();
        List<BlockPos> positions = List.of(chunkOrigin);
        for (PlacementModifier modifier : feature.placement()) {
            // Evaluated step by step: the modifiers draw from the one random in exactly this order.
            if (modifier instanceof BiomeFilter) {
                positions = positions.stream().filter(pos -> hasFeature(generator, randomState, feature, pos)).toList();
            } else {
                positions = positions.stream().flatMap(pos -> modifier.getPositions(context, random, pos)).toList();
            }
        }
        return positions.stream();
    }

    private static boolean hasFeature(ChunkGenerator generator, RandomState randomState, PlacedFeature feature, BlockPos pos) {
        Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
                QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()), randomState.sampler());
        return generator.getBiomeGenerationSettings(biome).hasFeature(feature);
    }
}
