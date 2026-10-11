package com.smartvillager.professions.farmer;

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
 * The Farmer's two stations (Obsidian Farmer.md, 2026-09-30), Farmer job sites like the composter (ProfessionsSections, core
 * station/ProfessionStations): the glazing station opens the Masteries (gold food -> dishes with an effect), the harvest basket the Passive
 * (counters -> golden crops, passive/HarvestWork). Both are still placeholder models.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class FarmerBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SmartVillagerProfessionsMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SmartVillagerProfessionsMod.MODID);

    /** Master workstation: gold food -> dishes with an effect (exchange/farmer_*.json). */
    public static final DeferredBlock<Block> GLAZING_STATION = BLOCKS.registerSimpleBlock("glazing_station",
            properties -> properties.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    /** Passive workstation: the golden crops land here (a basket block, basket/). */
    /** Decoration: lures Zombies (ScarecrowGoal). */
    public static final DeferredBlock<ScarecrowBlock> SCARECROW = BLOCKS.registerBlock("scarecrow", ScarecrowBlock::new,
            properties -> properties.mapColor(MapColor.COLOR_YELLOW).strength(1.0F).sound(SoundType.GRASS).noOcclusion());
    public static final DeferredBlock<BasketStationBlock> HARVEST_BASKET_STATION = BLOCKS.registerBlock("harvest_basket_station", BasketStationBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());

    public static final DeferredItem<BlockItem> GLAZING_STATION_ITEM = ITEMS.registerSimpleBlockItem(GLAZING_STATION);
    public static final DeferredItem<BlockItem> HARVEST_BASKET_STATION_ITEM = ITEMS.registerSimpleBlockItem(HARVEST_BASKET_STATION);
    public static final DeferredItem<BlockItem> SCARECROW_ITEM = ITEMS.registerSimpleBlockItem(SCARECROW);

    private FarmerBlocks() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            List.of(GLAZING_STATION_ITEM, HARVEST_BASKET_STATION_ITEM, SCARECROW_ITEM).forEach(event::accept);
        }
    }
}
