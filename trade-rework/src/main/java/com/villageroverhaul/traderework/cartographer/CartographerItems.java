package com.villageroverhaul.traderework.cartographer;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Cartographer's explore station and blank maps (Obsidian Cartographer.md, 2026-09-27). The maps are plain items that other professions
 * take as the second payment for an explorer map (trade/ExplorerMaps). Adventure map -> villages
 * (Cartographer), pillager outpost (Veteran), later geode (Mason) and ore veins (Salvager); challenge map ->
 * trial chambers, mansion, monument, ancient city (Veteran). Placeholder texture: Vanilla's empty map.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class CartographerItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeReworkMod.MODID);

    public static final DeferredItem<BlockItem> EXPLORE_STATION = ITEMS.registerSimpleBlockItem(CartographerBlocks.EXPLORE_STATION);
    public static final DeferredItem<Item> ADVENTURE_MAP = ITEMS.registerSimpleItem("adventure_map");
    public static final DeferredItem<Item> CHALLENGE_MAP = ITEMS.registerSimpleItem("challenge_map");

    private CartographerItems() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(EXPLORE_STATION);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            List.of(ADVENTURE_MAP, CHALLENGE_MAP).forEach(event::accept);
        }
    }
}
