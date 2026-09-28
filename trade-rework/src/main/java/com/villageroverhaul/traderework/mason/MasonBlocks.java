package com.villageroverhaul.traderework.mason;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Mason's blocks (Obsidian Mason.md, 2026-09-26). Rock pile, rock path and the crushing station have their Blockbench models
 * (assets/Mason), all others still use the placeholder model and
 * texture (assets/placeholder_block) - see "Fehlende Texturen und Modelle.md". The two stations are Mason
 * job sites like the stonecutter (TradeReworkSections, core station/ProfessionStations): the metamorph station opens the Masteries,
 * the crushing station the Passive. The villager statues (one per villager type, 2026-09-28) are grey villagers drawn by a block entity
 * renderer; a happiness effect is still open. The stone statue was dropped 2026-09-27 (it would have copied the armor stand).
 */
public final class MasonBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradeReworkMod.MODID);

    /**
     * New sandstone kind with Vanilla's whole sandstone family (2026-09-28). Textures (textures/block/brown_sand*,
     * *_brown_sandstone*; raw copies in assets/Mason/brown_sandstone) are Vanilla's sandstone and sand recolored:
     * stripped birch's average brightness at hue 38 degrees, then 10 % darker, 30 % more contrast, 25 % more
     * saturated, red channel x1.05.
     * Models, blockstates, loot, recipes and recipe unlocks are Vanilla's sandstone files with the ids swapped;
     * the properties match Vanilla's sandstone, smooth brown sandstone is harder like smooth sandstone.
     */
    /** Brown sand (2026-09-28): falls like sand, what the crushing station makes of brown sandstone, smelts to glass (item tag smelts_to_glass); dust in the recolored sand color. */
    public static final DeferredBlock<SandBlock> BROWN_SAND = BLOCKS.registerBlock("brown_sand", properties -> new SandBlock(new ColorRGBA(0xBD9E77), properties),
            properties -> properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND));
    public static final DeferredBlock<Block> BROWN_SANDSTONE = BLOCKS.registerSimpleBlock("brown_sandstone", MasonBlocks::sandstone);
    public static final DeferredBlock<StairBlock> BROWN_SANDSTONE_STAIRS = BLOCKS.registerBlock("brown_sandstone_stairs",
            properties -> new StairBlock(BROWN_SANDSTONE.get().defaultBlockState(), properties), MasonBlocks::sandstone);
    public static final DeferredBlock<SlabBlock> BROWN_SANDSTONE_SLAB = BLOCKS.registerBlock("brown_sandstone_slab", SlabBlock::new, MasonBlocks::sandstone);
    public static final DeferredBlock<WallBlock> BROWN_SANDSTONE_WALL = BLOCKS.registerBlock("brown_sandstone_wall", WallBlock::new,
            properties -> sandstone(properties).forceSolidOn());
    public static final DeferredBlock<Block> CHISELED_BROWN_SANDSTONE = BLOCKS.registerSimpleBlock("chiseled_brown_sandstone", MasonBlocks::sandstone);
    public static final DeferredBlock<Block> CUT_BROWN_SANDSTONE = BLOCKS.registerSimpleBlock("cut_brown_sandstone", MasonBlocks::sandstone);
    public static final DeferredBlock<SlabBlock> CUT_BROWN_SANDSTONE_SLAB = BLOCKS.registerBlock("cut_brown_sandstone_slab", SlabBlock::new, MasonBlocks::sandstone);
    public static final DeferredBlock<Block> SMOOTH_BROWN_SANDSTONE = BLOCKS.registerSimpleBlock("smooth_brown_sandstone", MasonBlocks::smoothSandstone);
    public static final DeferredBlock<SlabBlock> SMOOTH_BROWN_SANDSTONE_SLAB = BLOCKS.registerBlock("smooth_brown_sandstone_slab", SlabBlock::new,
            MasonBlocks::smoothSandstone);
    public static final DeferredBlock<StairBlock> SMOOTH_BROWN_SANDSTONE_STAIRS = BLOCKS.registerBlock("smooth_brown_sandstone_stairs",
            properties -> new StairBlock(SMOOTH_BROWN_SANDSTONE.get().defaultBlockState(), properties), MasonBlocks::smoothSandstone);
    /** The whole family in creative-tab order, like Vanilla's sandstone. */
    public static final List<DeferredBlock<? extends Block>> BROWN_SANDSTONE_FAMILY = List.of(BROWN_SANDSTONE, BROWN_SANDSTONE_STAIRS,
            BROWN_SANDSTONE_SLAB, BROWN_SANDSTONE_WALL, CHISELED_BROWN_SANDSTONE, SMOOTH_BROWN_SANDSTONE, SMOOTH_BROWN_SANDSTONE_STAIRS,
            SMOOTH_BROWN_SANDSTONE_SLAB, CUT_BROWN_SANDSTONE, CUT_BROWN_SANDSTONE_SLAB);

    /** Decorative rock pile: up to 4 rock groups per block, stacked like the book pile; drops only when mined with a pickaxe. */
    public static final DeferredBlock<RockPileBlock> ROCK_PILE = BLOCKS.registerBlock("rock_pile", RockPileBlock::new,
            properties -> properties.mapColor(MapColor.STONE).strength(1.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY));

    /** Flat path stones: up to 4 per block like the rock pile, sitting 1 px low for dirt paths, walked through - no collision, breaks instantly by hand (RockPathBlock). */
    public static final DeferredBlock<RockPathBlock> ROCK_PATH = BLOCKS.registerBlock("rock_path", RockPathBlock::new,
            properties -> properties.mapColor(MapColor.STONE).instabreak().sound(SoundType.STONE).noOcclusion().noCollision().pushReaction(PushReaction.DESTROY));

    /** Villager types with a statue, in creative-tab order - Vanilla's seven. */
    public static final List<ResourceKey<VillagerType>> STATUE_TYPES = List.of(VillagerType.PLAINS, VillagerType.DESERT, VillagerType.SAVANNA,
            VillagerType.TAIGA, VillagerType.SNOW, VillagerType.JUNGLE, VillagerType.SWAMP);
    /** One villager statue per type ("desert_villager_statue", ...), see VillagerStatueBlock. */
    public static final Map<ResourceKey<VillagerType>, DeferredBlock<VillagerStatueBlock>> VILLAGER_STATUES = STATUE_TYPES.stream()
            .collect(Collectors.toMap(type -> type, type -> BLOCKS.registerBlock(type.identifier().getPath() + "_villager_statue",
                    properties -> new VillagerStatueBlock(type, properties),
                    properties -> properties.mapColor(MapColor.STONE).strength(1.5F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                            .noOcclusion().pushReaction(PushReaction.BLOCK)),
                    (a, b) -> a, LinkedHashMap::new));

    /** Master workstation (placeholder cube for now). */
    public static final DeferredBlock<Block> METAMORPH_STATION = BLOCKS.registerSimpleBlock("metamorph_station",
            properties -> properties.mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE));
    /** Passive workstation: 3 input + 3 output slots, see CrushingStationBlockEntity. */
    public static final DeferredBlock<CrushingStationBlock> CRUSHING_STATION = BLOCKS.registerBlock("crushing_station",
            CrushingStationBlock::new,
            properties -> properties.mapColor(MapColor.STONE).strength(2.5F).sound(SoundType.STONE).noOcclusion());

    private MasonBlocks() {
    }

    /** Vanilla's sandstone, in a brown map color. */
    private static BlockBehaviour.Properties sandstone(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASEDRUM).strength(0.8F).requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }

    /** Vanilla's smooth sandstone: harder and blast resistant. */
    private static BlockBehaviour.Properties smoothSandstone(BlockBehaviour.Properties properties) {
        return sandstone(properties).strength(2.0F, 6.0F);
    }
}
