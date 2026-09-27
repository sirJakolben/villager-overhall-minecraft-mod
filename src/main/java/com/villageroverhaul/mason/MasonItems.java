package com.villageroverhaul.mason;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** Block items of every MasonBlocks block - they show up in trades and the creative inventory. */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class MasonItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VillagerOverhaulMod.MODID);

    public static final DeferredItem<BlockItem> BROWN_SANDSTONE = ITEMS.registerSimpleBlockItem(MasonBlocks.BROWN_SANDSTONE);
    public static final DeferredItem<BlockItem> ROCK_PILE = ITEMS.registerSimpleBlockItem(MasonBlocks.ROCK_PILE);
    public static final DeferredItem<BlockItem> ROCK_PATH = ITEMS.registerSimpleBlockItem(MasonBlocks.ROCK_PATH);
    public static final DeferredItem<BlockItem> VILLAGER_STATUE = ITEMS.registerSimpleBlockItem(MasonBlocks.VILLAGER_STATUE);
    public static final DeferredItem<BlockItem> METAMORPH_STATION = ITEMS.registerSimpleBlockItem(MasonBlocks.METAMORPH_STATION);
    public static final DeferredItem<BlockItem> CRUSHING_STATION = ITEMS.registerSimpleBlockItem(MasonBlocks.CRUSHING_STATION);

    private MasonItems() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            List.of(BROWN_SANDSTONE, ROCK_PILE, ROCK_PATH, VILLAGER_STATUE).forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            List.of(METAMORPH_STATION, CRUSHING_STATION).forEach(event::accept);
        }
    }
}
