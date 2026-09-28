package com.villageroverhaul.traderework.mason;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Block items of every MasonBlocks block - they show up in trades and the creative inventory. */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class MasonItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeReworkMod.MODID);

    public static final DeferredItem<BlockItem> BROWN_SANDSTONE = ITEMS.registerSimpleBlockItem(MasonBlocks.BROWN_SANDSTONE);
    public static final DeferredItem<BlockItem> ROCK_PILE = ITEMS.registerSimpleBlockItem(MasonBlocks.ROCK_PILE);
    public static final DeferredItem<BlockItem> ROCK_PATH = ITEMS.registerSimpleBlockItem(MasonBlocks.ROCK_PATH);
    /** The statue items, by villager type - the Mason's trade picks the one of his own type (trade/VillagerStatueOutput). */
    public static final Map<ResourceKey<VillagerType>, DeferredItem<BlockItem>> VILLAGER_STATUES = MasonBlocks.VILLAGER_STATUES.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> ITEMS.registerSimpleBlockItem(entry.getValue()), (a, b) -> a, LinkedHashMap::new));
    public static final DeferredItem<BlockItem> METAMORPH_STATION = ITEMS.registerSimpleBlockItem(MasonBlocks.METAMORPH_STATION);
    public static final DeferredItem<BlockItem> CRUSHING_STATION = ITEMS.registerSimpleBlockItem(MasonBlocks.CRUSHING_STATION);

    private MasonItems() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            List.of(BROWN_SANDSTONE, ROCK_PILE, ROCK_PATH).forEach(event::accept);
            VILLAGER_STATUES.values().forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            List.of(METAMORPH_STATION, CRUSHING_STATION).forEach(event::accept);
        }
    }
}
