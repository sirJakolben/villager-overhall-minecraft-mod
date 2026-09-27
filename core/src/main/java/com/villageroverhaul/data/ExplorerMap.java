package com.villageroverhaul.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import java.util.List;
import java.util.Optional;

/**
 * A trade whose output is an explorer map (2026-09-27, Cartographer / Veteran / Mason / Salvager): a filled
 * map to the nearest target, marked with decoration and named with the translation key name. Exactly one
 * target kind:
 * - destination: a structure tag, found like Vanilla's cartographer maps (villages, trial chambers ...)
 * - feature: one or more placed features, predicted from the world seed (amethyst geode, Trade Rework's explore/FeatureLocator);
 *   verify_block (optional) checks each candidate in the world first - dungeons: the spawner
 * - ore_vein: a big copper or iron vein, predicted from the terrain noise (Trade Rework's explore/OreVeinLocator)
 * Only the data shape lives in the core; the map itself is built by the extension (api/ExtensionHooks.explorerMaps) -
 * without it such an offer shows sold out.
 */
public record ExplorerMap(Optional<TagKey<Structure>> destination, Optional<List<ResourceKey<PlacedFeature>>> feature,
                          Optional<Block> verifyBlock, Optional<VeinType> oreVein, Holder<MapDecorationType> decoration, String name) {

    /** Vein kinds with Vanilla OreVeinifier's height band and toggle sign (copper: positive, y 0..50; iron: negative, y -60..-8). */
    public enum VeinType implements StringRepresentable {
        COPPER("copper", 0, 50, true),
        IRON("iron", -60, -8, false);

        public static final Codec<VeinType> CODEC = StringRepresentable.fromEnum(VeinType::values);

        private final String serializedName;
        private final int minY;
        private final int maxY;
        private final boolean positiveToggle;

        VeinType(String serializedName, int minY, int maxY, boolean positiveToggle) {
            this.serializedName = serializedName;
            this.minY = minY;
            this.maxY = maxY;
            this.positiveToggle = positiveToggle;
        }

        public int minY() {
            return minY;
        }

        public int maxY() {
            return maxY;
        }

        public boolean positiveToggle() {
            return positiveToggle;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

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

    /** Structures are only drawn from above; geodes and veins are underground, so the map says how deep. */
    public boolean showsDepth() {
        return destination.isEmpty();
    }
}
