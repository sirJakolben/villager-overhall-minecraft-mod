package com.villageroverhaul.traderework.cartographer;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Cartographer's station (Obsidian Cartographer.md, 2026-09-27), placeholder model and texture for now.
 * A Cartographer job site like the cartography table (claim/ProfessionStations) that opens the Masteries -
 * the village maps. No passive station yet.
 */
public final class CartographerBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradeReworkMod.MODID);

    /** Master workstation (placeholder cube for now). */
    public static final DeferredBlock<Block> EXPLORE_STATION = BLOCKS.registerSimpleBlock("explore_station",
            properties -> properties.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD));

    private CartographerBlocks() {
    }
}
