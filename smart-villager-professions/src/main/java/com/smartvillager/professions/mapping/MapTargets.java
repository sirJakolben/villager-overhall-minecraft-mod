package com.smartvillager.professions.mapping;

import com.mojang.datafixers.util.Pair;
import com.smartvillager.professions.ProfessionsRegistries;
import com.smartvillager.professions.trade.ExplorerMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Where the passive stations' maps point (2026-10-10, Cartographer: adventure pool, Veteran: challenge pool). A map is drawn when a
 * station's meter is full: the candidates of the pool are the entries of every map_pool file of that kind (MapPool) plus every other
 * structure of the world that has no entry anywhere and generates in this dimension (fallback - underground and stronghold-step
 * structures go to the challenge pool, all others to the adventure pool, weight 1). One candidate is drawn by weight and searched like
 * Vanilla's cartographer does (radius 100 chunks, only structures no other map points to yet, ExplorerMaps.SEARCH_RADIUS_CHUNKS);
 * if there is none, the next draw follows, up to MAX_SEARCHES per call - the searches are the expensive part, so the caller waits a
 * while before trying again.
 */
public final class MapTargets {

    public static final int MAX_SEARCHES = 3;

    private MapTargets() {
    }

    /** A drawn candidate: where to look, and the weight it was drawn with. */
    private record Candidate(HolderSet<Structure> structures, int weight) {
    }

    /** The finished map for the first drawn candidate with a structure in reach, or empty. */
    public static Optional<ItemStack> draw(ServerLevel level, MapPool.Kind kind, BlockPos origin, RandomSource random, String nameKey) {
        List<Candidate> candidates = candidates(level, kind);
        for (int search = 0; search < MAX_SEARCHES && !candidates.isEmpty(); search++) {
            Candidate drawn = pick(candidates, random);
            candidates.remove(drawn);
            Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, drawn.structures(), origin, ExplorerMaps.SEARCH_RADIUS_CHUNKS, true);
            if (found != null) {
                ItemStack map = ExplorerMaps.createMap(level, found.getFirst(), MapDecorationTypes.TARGET_X, nameKey);
                map.set(DataComponents.ITEM_NAME, Component.translatable(nameKey, structureName(found.getSecond())));
                return Optional.of(map);
            }
        }
        return Optional.empty();
    }

    /** "structure.<namespace>.<path>" if a language file has it (the mod ships the Vanilla ones), otherwise the path made readable (modded structures). */
    static Component structureName(Holder<Structure> structure) {
        Identifier id = structure.unwrapKey().map(ResourceKey::identifier).orElse(null);
        if (id == null) {
            return Component.translatable("structure.smartvillager_professions.unknown");
        }
        StringBuilder readable = new StringBuilder();
        for (String word : id.getPath().replace('/', '_').split("_")) {
            if (!word.isEmpty()) {
                readable.append(readable.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return Component.translatableWithFallback("structure." + id.getNamespace() + "." + id.getPath().replace('/', '.'), readable.toString());
    }

    private static Candidate pick(List<Candidate> candidates, RandomSource random) {
        int total = candidates.stream().mapToInt(Candidate::weight).sum();
        int roll = random.nextInt(total);
        for (Candidate candidate : candidates) {
            roll -= candidate.weight();
            if (roll < 0) {
                return candidate;
            }
        }
        return candidates.getLast();
    }

    private static List<Candidate> candidates(ServerLevel level, MapPool.Kind kind) {
        if (!level.getServer().getWorldGenSettings().options().generateStructures()) {
            return new ArrayList<>();
        }
        ChunkGeneratorStructureState generator = level.getChunkSource().getGeneratorState();
        Registry<MapPool> pools = level.registryAccess().lookupOrThrow(ProfessionsRegistries.MAP_POOL);
        List<Candidate> candidates = new ArrayList<>();
        Set<Holder<Structure>> listed = new HashSet<>();
        for (MapPool pool : pools) {
            for (MapPool.Entry entry : pool.entries()) {
                entry.structures().forEach(listed::add);
                // A listed entry whose structures do not generate here (a nether fortress in the overworld) can never be found - skip it now, not after a search.
                if (pool.kind() == kind && entry.structures().stream().anyMatch(s -> generates(generator, s))) {
                    candidates.add(new Candidate(entry.structures(), entry.weight()));
                }
            }
        }
        for (Holder<Structure> structure : level.registryAccess().lookupOrThrow(Registries.STRUCTURE).listElements().toList()) {
            if (!listed.contains(structure) && fallbackKind(structure.value()) == kind && generates(generator, structure)) {
                candidates.add(new Candidate(HolderSet.direct(structure), 1));
            }
        }
        return candidates;
    }

    private static boolean generates(ChunkGeneratorStructureState generator, Holder<Structure> structure) {
        return !generator.getPlacementsForStructure(structure).isEmpty();
    }

    /** What a structure without an entry is: below the surface -> challenge, on it -> adventure (the decided default rule). */
    static MapPool.Kind fallbackKind(Structure structure) {
        GenerationStep.Decoration step = structure.step();
        return step == GenerationStep.Decoration.UNDERGROUND_STRUCTURES || step == GenerationStep.Decoration.STRONGHOLDS
                ? MapPool.Kind.CHALLENGE : MapPool.Kind.ADVENTURE;
    }
}
