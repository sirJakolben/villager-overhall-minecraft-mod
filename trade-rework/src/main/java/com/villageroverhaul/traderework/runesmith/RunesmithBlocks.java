package com.villageroverhaul.traderework.runesmith;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Runesmith's two stations (Obsidian Runesmith.md; built 2026-09-26 for the old Repair Smith, moved to
 * the Runesmith 2026-09-27), placeholder model and texture for now. Both are Runesmith job sites like the
 * smithing table (claim/ProfessionStations): the upgrade station
 * opens the Masteries, the repair station the Passive.
 */
public final class RunesmithBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradeReworkMod.MODID);

    /** Master workstation (placeholder cube for now). */
    public static final DeferredBlock<Block> UPGRADE_STATION = BLOCKS.registerSimpleBlock("upgrade_station",
            properties -> properties.mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL));
    /** Passive workstation: 3 input + 3 output slots, see RepairStationBlockEntity. */
    public static final DeferredBlock<RepairStationBlock> REPAIR_STATION = BLOCKS.registerBlock("repair_station",
            RepairStationBlock::new,
            properties -> properties.mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL));

    private RunesmithBlocks() {
    }
}
