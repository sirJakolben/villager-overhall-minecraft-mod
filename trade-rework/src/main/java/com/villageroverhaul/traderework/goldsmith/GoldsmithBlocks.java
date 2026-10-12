package com.villageroverhaul.traderework.goldsmith;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Goldsmith's two stations (Obsidian Goldsmith.md, 2026-09-27), Goldsmith job sites (TradeReworkSections, core station/ProfessionStations)
 * that open the Masteries and the Passive. The salvage station works (passive/SalvageWork); the gildingent
 * station is still a placeholder cube, its Masteries (block + metal, exchange/goldsmith_*.json) make the blocks below.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class GoldsmithBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradeReworkMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeReworkMod.MODID);

    /** Master workstation: metal-decorated blocks (placeholder cube for now). */
    public static final DeferredBlock<Block> GILDING_STATION = BLOCKS.registerSimpleBlock("gilding_station",
            properties -> properties.mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL));
    /** Passive workstation: metal gear back to ingots (SalvageStationBlockEntity, passive/SalvageWork). */
    public static final DeferredBlock<SalvageStationBlock> SALVAGE_STATION = BLOCKS.registerBlock("salvage_station", SalvageStationBlock::new,
            properties -> properties.mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(state -> state.getValue(SalvageStationBlock.WORKING) ? SalvageStationBlock.WORKING_LIGHT : 0));

    /*
     * Basic 0 + 1 (2026-09-29): emerald + Vanilla chain / bars -> the gilded kind. Seabrass chain and bars from Abyssal Decor by
     * starrysock (CC0, used with the author's permission) on Vanilla's chain and bars templates; they behave like the iron ones.
     */
    public static final DeferredBlock<ChainBlock> GILDED_IRON_CHAIN = BLOCKS.registerBlock("gilded_iron_chain", ChainBlock::new,
            properties -> properties.forceSolidOn().requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.CHAIN).noOcclusion());
    public static final DeferredBlock<IronBarsBlock> GILDED_IRON_BARS = BLOCKS.registerBlock("gilded_iron_bars", IronBarsBlock::new,
            properties -> properties.requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.IRON).noOcclusion());

    /*
     * The Masteries' blocks (2026-09-29): block + metal at the gilding station. The iron frame is from Abyssal Decor by
     * starrysock (CC0, used with the author's permission); the reinforced glass puts Vanilla's glass inside that frame (raw
     * copy in assets/Goldsmith). Gilded blackstone is Vanilla's.
     */
    /**
     * Master rank 1: glass in an iron frame - see-through like glass, but drops itself and resists blasts like stone; the
     * frame connects to neighbouring reinforced glass (ReinforcedGlassBlock, textures in assets/Goldsmith/reinforced_glass).
     */
    public static final DeferredBlock<ReinforcedGlassBlock> REINFORCED_GLASS = BLOCKS.registerBlock("reinforced_glass", ReinforcedGlassBlock::new,
            properties -> properties.instrument(NoteBlockInstrument.HAT).strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.GLASS)
                    .noOcclusion().isValidSpawn((state, level, pos, entity) -> false).isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false));
    /**
     * Master rank 2: a redstone block in an iron frame, powering like the redstone block. The stonecutter makes stairs, slab,
     * ornated redstone and ornated redstone pillar from it (2026-09-30, drawn in-house, raw files in assets/Goldsmith/redstone
     * variants) - pure decoration without a signal, so they can be built into walls next to lamps and doors. The stairs use the
     * gold-framed lapis stairs' models with the redstone textures.
     */
    public static final DeferredBlock<PoweredBlock> IRON_FRAMED_REDSTONE = BLOCKS.registerBlock("iron_framed_redstone", PoweredBlock::new,
            properties -> redstone(properties).isRedstoneConductor((state, level, pos) -> false));
    public static final DeferredBlock<SlabBlock> IRON_FRAMED_REDSTONE_SLAB = BLOCKS.registerBlock("iron_framed_redstone_slab", SlabBlock::new,
            GoldsmithBlocks::redstone);
    public static final DeferredBlock<StairBlock> IRON_FRAMED_REDSTONE_STAIRS = BLOCKS.registerBlock("iron_framed_redstone_stairs",
            properties -> new StairBlock(IRON_FRAMED_REDSTONE.get().defaultBlockState(), properties), GoldsmithBlocks::redstone);
    public static final DeferredBlock<OrnatedRedstoneBlock> ORNATED_REDSTONE = BLOCKS.registerBlock("ornated_redstone", OrnatedRedstoneBlock::new,
            GoldsmithBlocks::redstone);
    public static final DeferredBlock<RotatedPillarBlock> ORNATED_REDSTONE_PILLAR = BLOCKS.registerBlock("ornated_redstone_pillar",
            RotatedPillarBlock::new, GoldsmithBlocks::redstone);
    /**
     * Master rank 3: the gold-framed lapis, drawn in-house like the ornated lapis and pillar (2026-09-29, raw files in
     * assets/Goldsmith/lapis variants; the plain Abyssal Decor lapis variants were removed the same day). The stonecutter
     * makes the rest from it (recipe/*_from_gold_framed_lapis_stonecutting.json); the crushing station takes them back.
     * The slab and stairs use the gold-framed texture on every face.
     */
    public static final DeferredBlock<Block> GOLD_FRAMED_LAPIS = BLOCKS.registerSimpleBlock("gold_framed_lapis", GoldsmithBlocks::lapis);
    public static final DeferredBlock<SlabBlock> GOLD_FRAMED_LAPIS_SLAB = BLOCKS.registerBlock("gold_framed_lapis_slab", SlabBlock::new,
            GoldsmithBlocks::lapis);
    public static final DeferredBlock<StairBlock> GOLD_FRAMED_LAPIS_STAIRS = BLOCKS.registerBlock("gold_framed_lapis_stairs",
            properties -> new StairBlock(GOLD_FRAMED_LAPIS.get().defaultBlockState(), properties), GoldsmithBlocks::lapis);
    public static final DeferredBlock<OrnatedBlock> ORNATED_LAPIS = BLOCKS.registerBlock("ornated_lapis", OrnatedBlock::new,
            GoldsmithBlocks::lapis);
    public static final DeferredBlock<RotatedPillarBlock> ORNATED_LAPIS_PILLAR = BLOCKS.registerBlock("ornated_lapis_pillar", RotatedPillarBlock::new,
            GoldsmithBlocks::lapis);

    /** The trades' deco blocks in creative-tab order. */
    private static final List<DeferredBlock<? extends Block>> DECO_BLOCKS = List.of(GILDED_IRON_CHAIN, GILDED_IRON_BARS, REINFORCED_GLASS, IRON_FRAMED_REDSTONE,
            IRON_FRAMED_REDSTONE_STAIRS, IRON_FRAMED_REDSTONE_SLAB, ORNATED_REDSTONE, ORNATED_REDSTONE_PILLAR, GOLD_FRAMED_LAPIS, GOLD_FRAMED_LAPIS_STAIRS, GOLD_FRAMED_LAPIS_SLAB, ORNATED_LAPIS, ORNATED_LAPIS_PILLAR);

    public static final DeferredItem<BlockItem> GILDING_STATION_ITEM = ITEMS.registerSimpleBlockItem(GILDING_STATION);
    public static final DeferredItem<BlockItem> SALVAGE_STATION_ITEM = ITEMS.registerSimpleBlockItem(SALVAGE_STATION);
    private static final List<DeferredItem<BlockItem>> DECO_BLOCK_ITEMS = DECO_BLOCKS.stream()
            .map(block -> ITEMS.registerSimpleBlockItem(block)).toList();

    private GoldsmithBlocks() {
    }

    /** Vanilla's redstone block: hard, metal sound, in the redstone map color. */
    private static BlockBehaviour.Properties redstone(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.FIRE).strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL);
    }

    /** Stone-like, in the lapis map color. */
    private static BlockBehaviour.Properties lapis(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.LAPIS).instrument(NoteBlockInstrument.BASEDRUM).strength(1.5F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            List.of(GILDING_STATION_ITEM, SALVAGE_STATION_ITEM).forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            DECO_BLOCK_ITEMS.forEach(event::accept);
        }
    }
}
