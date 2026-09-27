package com.villageroverhaul.veteran;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Veteran's station and weapon rack (Obsidian Veteran.md, 2026-09-27), placeholder model and texture for now. A Veteran
 * job site like the grindstone (claim/ProfessionStations) that opens the Masteries - the challenge maps.
 * The Veteran has no passive station yet.
 */
public final class VeteranBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VillagerOverhaulMod.MODID);

    /** Master workstation (placeholder cube for now). */
    public static final DeferredBlock<Block> CHALLENGE_STATION = BLOCKS.registerSimpleBlock("challenge_station",
            properties -> properties.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD));

    /** Wall rack for one weapon (WeaponRackBlock), placeholder look for now. */
    public static final DeferredBlock<WeaponRackBlock> WEAPON_RACK = BLOCKS.registerBlock("weapon_rack", WeaponRackBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion().noCollision());

    private VeteranBlocks() {
    }
}
