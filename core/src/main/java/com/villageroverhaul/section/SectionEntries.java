package com.villageroverhaul.section;

import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.data.ModDataPackRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The data-pack entries (data/<namespace>/villageroverhaul/exchange/) grouped by profession and section, each
 * group in unlock order (rank, then id - so every upgrade appends its entries at the bottom). Built once per
 * loaded registry object and reused until a data-pack reload replaces it - kept per registry, because in single
 * player the client and the integrated server each ask with their own copy.
 */
public final class SectionEntries {

    public record Entry(Identifier id, ItemExchange exchange) {
    }

    private record Index(Map<VillagerProfession, Map<Identifier, List<Entry>>> byProfession) {
    }

    private static final Comparator<Entry> UNLOCK_ORDER = Comparator.comparingInt((Entry entry) -> entry.exchange().unlockRank())
            .thenComparing(entry -> entry.id().toString());

    /** Weak keys: a registry replaced by a reload is dropped with its index. */
    private static final Map<Registry<ItemExchange>, Index> INDEXES = Collections.synchronizedMap(new WeakHashMap<>());

    private SectionEntries() {
    }

    public static List<Entry> of(RegistryAccess registries, VillagerProfession profession, Identifier section) {
        return index(registries).byProfession().getOrDefault(profession, Map.of()).getOrDefault(section, List.of());
    }

    /** The profession has data-pack entries of its own - it is designed, not built from its Vanilla trades. */
    public static boolean hasAny(RegistryAccess registries, VillagerProfession profession) {
        return index(registries).byProfession().containsKey(profession);
    }

    private static Index index(RegistryAccess registries) {
        return INDEXES.computeIfAbsent(registries.lookupOrThrow(ModDataPackRegistries.EXCHANGE), SectionEntries::build);
    }

    private static Index build(Registry<ItemExchange> registry) {
        Map<VillagerProfession, Map<Identifier, List<Entry>>> byProfession = new HashMap<>();
        for (var entry : registry.entrySet()) {
            ItemExchange exchange = entry.getValue();
            byProfession.computeIfAbsent(exchange.profession(), p -> new HashMap<>())
                    .computeIfAbsent(exchange.section(), s -> new ArrayList<>())
                    .add(new Entry(entry.getKey().identifier(), exchange));
        }
        byProfession.values().forEach(sections -> sections.replaceAll((section, entries) -> entries.stream().sorted(UNLOCK_ORDER).toList()));
        return new Index(byProfession);
    }
}
