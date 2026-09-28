package com.villageroverhaul.traderework.mason;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Mason's blocks (Obsidian Mason.md, 2026-09-26). Rock pile and rock path have their Blockbench models
 * (assets/Mason), all others still use the placeholder model and
 * texture (assets/placeholder_block) - see "Fehlende Texturen und Modelle.md". The two stations are Mason
 * job sites like the stonecutter (TradeReworkSections, core station/ProfessionStations): the metamorph station opens the Masteries,
 * the crushing station the Passive. The villager statue is a plain decoration block for now; its final form
 * (happiness effect) is still open. The stone statue was dropped 2026-09-27 (it would have copied the armor stand).
 */
public final class MasonBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradeReworkMod.MODID);

    /** New sandstone kind, about oak planks in color and brightness; only the base block so far. */
    public static final DeferredBlock<Block> BROWN_SANDSTONE = BLOCKS.registerSimpleBlock("brown_sandstone",
            properties -> properties.mapColor(MapColor.WOOD).strength(0.8F).requiresCorrectToolForDrops().sound(SoundType.STONE));

    /** Decorative rock pile: up to 4 rock groups per block, stacked like the book pile; drops only when mined with a pickaxe. */
    public static final DeferredBlock<RockPileBlock> ROCK_PILE = BLOCKS.registerBlock("rock_pile", RockPileBlock::new,
            properties -> properties.mapColor(MapColor.STONE).strength(1.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY));

    /** Flat path stones: up to 4 per block like the rock pile, sitting 1 px low for dirt paths, walked through - no collision, breaks instantly by hand (RockPathBlock). */
    public static final DeferredBlock<RockPathBlock> ROCK_PATH = BLOCKS.registerBlock("rock_path", RockPathBlock::new,
            properties -> properties.mapColor(MapColor.STONE).instabreak().sound(SoundType.STONE).noOcclusion().noCollision().pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<Block> VILLAGER_STATUE = BLOCKS.registerSimpleBlock("villager_statue",
            properties -> properties.mapColor(MapColor.STONE).strength(1.5F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion());

    /** Master workstation (placeholder cube for now). */
    public static final DeferredBlock<Block> METAMORPH_STATION = BLOCKS.registerSimpleBlock("metamorph_station",
            properties -> properties.mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE));
    /** Passive workstation: 3 input + 3 output slots, see CrushingStationBlockEntity. */
    public static final DeferredBlock<CrushingStationBlock> CRUSHING_STATION = BLOCKS.registerBlock("crushing_station",
            CrushingStationBlock::new,
            properties -> properties.mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE));

    private MasonBlocks() {
    }
}
