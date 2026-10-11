package com.smartvillager.professions.cartographer;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
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
 * The Cartographer's blank maps (Obsidian Cartographer.md, 2026-09-27; since 2026-10-10 sold by his Masteries). The maps are plain items: the Adventure map
 * is the second payment for the Mason's geode and the Goldsmith's ore vein maps (trade/ExplorerMaps) and what the Adventure Station draws from, the Challenge map
 * what the Veteran's Challenge Station draws from (mapping/). Placeholder texture: Vanilla's empty map.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class CartographerItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SmartVillagerProfessionsMod.MODID);

    public static final DeferredItem<BlockItem> MAP_STATION = ITEMS.registerSimpleBlockItem(CartographerBlocks.MAP_STATION);
    public static final DeferredItem<Item> ADVENTURE_MAP = ITEMS.registerSimpleItem("adventure_map");
    public static final DeferredItem<Item> CHALLENGE_MAP = ITEMS.registerSimpleItem("challenge_map");

    private CartographerItems() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(MAP_STATION);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            List.of(ADVENTURE_MAP, CHALLENGE_MAP).forEach(event::accept);
        }
    }
}
