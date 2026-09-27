package com.villageroverhaul.traderework.trade;

import com.villageroverhaul.api.ExtensionHooks.MapOutput;
import com.mojang.serialization.Codec;
import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.data.ExplorerMap;
import com.villageroverhaul.traderework.explore.FeatureLocator;
import com.villageroverhaul.traderework.explore.OreVeinLocator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Explorer-map trades (2026-09-27, Cartographer / Veteran / Mason / Salvager), plugged into the core's offer list
 * through api/ExtensionHooks.setExplorerMaps (TradeReworkMod). Structures are searched like
 * Vanilla's; geodes (explore/FeatureLocator) and ore veins (explore/OreVeinLocator) are predicted from the
 * world seed within SEED_SEARCH_RADIUS_CHUNKS, and their maps name the height in the tooltip. Vanilla searches the structure when it
 * generates the offer; our offer list is rebuilt on every state change, so the search runs once per
 * villager and trade - the first time the list is built while a player trades with it - and the finished
 * map is kept on the villager (MAPS, saved). Every purchase then hands out a copy of that same map, exactly
 * like one Vanilla map offer. Same search as Vanilla's cartographer (ExplorationMapFunction): radius 100
 * chunks, only structures no other map points to yet.
 *
 * Without a trading player (list rebuilt in the background) the offer shows a blank placeholder map and
 * nothing is searched. No structure found: the offer shows sold out; that result is remembered (not saved)
 * for RETRY_TICKS, so the many list rebuilds while trading don't search again and again.
 */
public final class ExplorerMaps {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TradeReworkMod.MODID);

    private static final Codec<Map<Identifier, ItemStack>> MAPS_CODEC = Codec.unboundedMap(Identifier.CODEC, ItemStack.CODEC);

    /** Found maps by trade id - one per explorer-map trade of this villager. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Map<Identifier, ItemStack>>> MAPS = ATTACHMENT_TYPES.register(
            "explorer_maps",
            () -> AttachmentType.<Map<Identifier, ItemStack>>builder(() -> Map.of())
                    .serialize(MAPS_CODEC.fieldOf("maps"))
                    .build()
    );

    private static final int SEARCH_RADIUS_CHUNKS = 100;
    /** Geodes and ore veins are common - a smaller radius keeps the seed search short. */
    private static final int SEED_SEARCH_RADIUS_CHUNKS = 32;
    private static final byte MAP_SCALE = 2;
    /** How long a failed search is remembered before it is tried again (one day). */
    private static final long RETRY_TICKS = 24000L;

    /** Failed searches: villager -> trade -> game time of the search. In memory only. */
    private static final Map<UUID, Map<Identifier, Long>> NOT_FOUND = new ConcurrentHashMap<>();


    private ExplorerMaps() {
    }

    public static MapOutput outputFor(Villager villager, Identifier tradeId, ExplorerMap spec) {
        ItemStack cached = villager.getData(MAPS).get(tradeId);
        if (cached != null) {
            return new MapOutput(cached.copy(), false);
        }
        if (!(villager.level() instanceof ServerLevel level) || villager.getTradingPlayer() == null) {
            return new MapOutput(placeholder(spec), false);
        }
        Long failedAt = NOT_FOUND.getOrDefault(villager.getUUID(), Map.of()).get(tradeId);
        if (failedAt != null && level.getGameTime() - failedAt < RETRY_TICKS) {
            return new MapOutput(placeholder(spec), true);
        }
        Optional<BlockPos> found = findTarget(level, spec, villager.blockPosition());
        if (found.isEmpty()) {
            NOT_FOUND.computeIfAbsent(villager.getUUID(), uuid -> new ConcurrentHashMap<>()).put(tradeId, level.getGameTime());
            return new MapOutput(placeholder(spec), true);
        }
        BlockPos target = found.get();
        ItemStack map = MapItem.create(level, target.getX(), target.getZ(), MAP_SCALE, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, target, "+", spec.decoration());
        map.set(DataComponents.ITEM_NAME, Component.translatable(spec.name()));
        if (spec.showsDepth()) {
            map.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("filled_map.vo_trade_rework.depth", target.getY()).withStyle(ChatFormatting.GRAY))));
        }

        Map<Identifier, ItemStack> maps = new HashMap<>(villager.getData(MAPS));
        maps.put(tradeId, map);
        villager.setData(MAPS, Map.copyOf(maps));
        return new MapOutput(map.copy(), false);
    }

    private static Optional<BlockPos> findTarget(ServerLevel level, ExplorerMap spec, BlockPos origin) {
        if (spec.destination().isPresent()) {
            return Optional.ofNullable(level.findNearestMapStructure(spec.destination().get(), origin, SEARCH_RADIUS_CHUNKS, true));
        }
        if (spec.feature().isPresent()) {
            return FeatureLocator.findNearest(level, spec.feature().get(), origin, SEED_SEARCH_RADIUS_CHUNKS, spec.verifyBlock());
        }
        return OreVeinLocator.findNearest(level, spec.oreVein().orElseThrow(), origin, SEED_SEARCH_RADIUS_CHUNKS);
    }

    /** Blank map with the offer's name, shown until the real map exists. */
    private static ItemStack placeholder(ExplorerMap spec) {
        ItemStack stack = new ItemStack(Items.MAP);
        stack.set(DataComponents.ITEM_NAME, Component.translatable(spec.name()));
        return stack;
    }
}
