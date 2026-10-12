package com.smartvillager.professions.fisherman;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.basket.BasketStationBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Fisherman's two stations (Obsidian Fisherman.md, 2026-09-30), Fisherman job sites like the barrel (ProfessionsSections, core
 * station/ProfessionStations): the shipyard station opens the Masteries (boat building), the fish basket the Passive (catches and biome fish,
 * passive/FishingWork). Both are still placeholder models.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class FishermanBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SmartVillagerProfessionsMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SmartVillagerProfessionsMod.MODID);

    /** Master workstation: boat building (exchange/fisherman_*.json). */
    public static final DeferredBlock<Block> SHIPYARD_STATION = BLOCKS.registerSimpleBlock("shipyard_station",
            properties -> properties.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion());
    /** Passive workstation: the catch lands here (a basket block, basket/). */
    public static final DeferredBlock<BasketStationBlock> FISH_BASKET_STATION = BLOCKS.registerBlock("fish_basket_station", BasketStationBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());

    public static final DeferredItem<BlockItem> SHIPYARD_STATION_ITEM = ITEMS.registerSimpleBlockItem(SHIPYARD_STATION);
    public static final DeferredItem<BlockItem> FISH_BASKET_STATION_ITEM = ITEMS.registerSimpleBlockItem(FISH_BASKET_STATION);

    private FishermanBlocks() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            List.of(SHIPYARD_STATION_ITEM, FISH_BASKET_STATION_ITEM).forEach(event::accept);
        }
    }
}
