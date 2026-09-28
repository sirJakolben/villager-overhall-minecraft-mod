package com.villageroverhaul.traderework.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.api.ExchangeExtensionType;
import com.villageroverhaul.api.ResultOverride;
import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.explore.VeinType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import java.util.List;
import java.util.Optional;

/**
 * An entry whose output is an explorer map (2026-09-27, Cartographer / Veteran / Mason / Salvager), under
 * "extensions": {"vo_trade_rework:explorer_map": {...}}: a filled map to the nearest target, marked with
 * decoration and named with the translation key name. Exactly one target kind:
 * - destination: a structure tag, found like Vanilla's cartographer maps (villages, trial chambers ...)
 * - feature: one or more placed features, predicted from the world seed (amethyst geode, explore/FeatureLocator);
 *   verify_block (optional) checks each candidate in the world first - dungeons: the spawner
 * - ore_vein: a big copper or iron vein, predicted from the terrain noise (explore/OreVeinLocator)
 * The map replaces the entry's base_output (api/ResultOverride), built by ExplorerMaps.
 */
public record ExplorerMap(Optional<TagKey<Structure>> destination, Optional<List<ResourceKey<PlacedFeature>>> feature,
                          Optional<Block> verifyBlock, Optional<VeinType> oreVein, Holder<MapDecorationType> decoration, String name)
        implements ResultOverride {

    public static final Codec<ExplorerMap> CODEC = RecordCodecBuilder.<ExplorerMap>create(instance -> instance.group(
            TagKey.codec(Registries.STRUCTURE).optionalFieldOf("destination").forGetter(ExplorerMap::destination),
            ExtraCodecs.compactListCodec(ResourceKey.codec(Registries.PLACED_FEATURE)).optionalFieldOf("feature").forGetter(ExplorerMap::feature),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("verify_block").forGetter(ExplorerMap::verifyBlock),
            VeinType.CODEC.optionalFieldOf("ore_vein").forGetter(ExplorerMap::oreVein),
            MapDecorationType.CODEC.fieldOf("decoration").forGetter(ExplorerMap::decoration),
            Codec.STRING.fieldOf("name").forGetter(ExplorerMap::name)
    ).apply(instance, ExplorerMap::new)).validate(map -> {
        int targets = (map.destination.isPresent() ? 1 : 0) + (map.feature.isPresent() ? 1 : 0) + (map.oreVein.isPresent() ? 1 : 0);
        return targets == 1 ? DataResult.success(map)
                : DataResult.error(() -> "explorer_map needs exactly one of destination, feature, ore_vein");
    });

    public static final ExchangeExtensionType<ExplorerMap> TYPE =
            new ExchangeExtensionType<>(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "explorer_map"), CODEC);

    /** Structures are only drawn from above; geodes and veins are underground, so the map says how deep. */
    public boolean showsDepth() {
        return destination.isEmpty();
    }

    @Override
    public Output resultFor(Villager villager, Identifier entryId) {
        return ExplorerMaps.outputFor(villager, entryId, this);
    }
}
