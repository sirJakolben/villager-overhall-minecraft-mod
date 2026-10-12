package com.villageroverhaul.traderework.mason;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
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

    public static final DeferredItem<BlockItem> BROWN_SAND = ITEMS.registerSimpleBlockItem(MasonBlocks.BROWN_SAND);
    /** Brown sandstone and its variants, in the order of MasonBlocks.BROWN_SANDSTONE_FAMILY. */
    public static final List<DeferredItem<BlockItem>> BROWN_SANDSTONE_FAMILY = MasonBlocks.BROWN_SANDSTONE_FAMILY.stream()
            .map(ITEMS::registerSimpleBlockItem).toList();
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
            // Right after Vanilla's red sandstone family, in the same order as Vanilla's sandstone.
            insertAfter(event, Items.CUT_RED_SANDSTONE_SLAB, BROWN_SANDSTONE_FAMILY);
            List.of(ROCK_PILE, ROCK_PATH).forEach(event::accept);
            VILLAGER_STATUES.values().forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            insertAfter(event, Items.RED_SANDSTONE, List.of(BROWN_SAND, BROWN_SANDSTONE_FAMILY.getFirst()));
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            List.of(METAMORPH_STATION, CRUSHING_STATION).forEach(event::accept);
        }
    }

    /** Puts the items one after another behind anchor, so they show up next to their Vanilla relatives instead of at the tab's end. */
    private static void insertAfter(BuildCreativeModeTabContentsEvent event, ItemLike anchor, List<? extends ItemLike> items) {
        ItemLike previous = anchor;
        for (ItemLike item : items) {
            event.insertAfter(new ItemStack(previous), new ItemStack(item), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            previous = item;
        }
    }
}
