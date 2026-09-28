package com.villageroverhaul.traderework.salvager;

import com.villageroverhaul.traderework.TradeReworkMod;
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
 * The Salvager's two stations (Obsidian Salvager.md, 2026-09-27), Salvager job sites (TradeReworkSections, core station/ProfessionStations)
 * that open the Masteries and the Passive. The smelting station works (passive/SmeltingWork); the refinement
 * station is still a placeholder without a function.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class SalvagerBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradeReworkMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeReworkMod.MODID);

    /** Master workstation: metal-decorated blocks (planned). */
    public static final DeferredBlock<Block> REFINEMENT_STATION = BLOCKS.registerSimpleBlock("refinement_station",
            properties -> properties.mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL));
    /** Passive workstation: metal gear back to ingots (SmeltingStationBlockEntity, passive/SmeltingWork). */
    public static final DeferredBlock<SmeltingStationBlock> SMELTING_STATION = BLOCKS.registerBlock("smelting_station", SmeltingStationBlock::new,
            properties -> properties.mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(SmeltingStationBlock.WORKING) ? SmeltingStationBlock.WORKING_LIGHT : 0));

    public static final DeferredItem<BlockItem> REFINEMENT_STATION_ITEM = ITEMS.registerSimpleBlockItem(REFINEMENT_STATION);
    public static final DeferredItem<BlockItem> SMELTING_STATION_ITEM = ITEMS.registerSimpleBlockItem(SMELTING_STATION);

    private SalvagerBlocks() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            List.of(REFINEMENT_STATION_ITEM, SMELTING_STATION_ITEM).forEach(event::accept);
        }
    }
}
