package com.villageroverhaul.traderework.librarian;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Librarian's two extra workstations as placeable blocks (2026-09-24). Their Blockbench models are
 * not full cubes, hence noOcclusion (neighbors keep their faces) and hitboxes built from the model's
 * elements (models/block/*.json, in 1/16 block). Both are Librarian job sites like the lectern (see
 * claim/ProfessionStations); owning one opens the Masteries / Passive group. The enchantment station is
 * also a 5-slot book container (EnchantmentStationBlock), the writing station a plain ModelShapedBlock.
 */
public final class LibrarianBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradeReworkMod.MODID);

    /** Base slab, inset body, two staggered boards (low right, high left) - the rotated ink pot has no hitbox. */
    private static final VoxelShape WRITING_STATION_SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(1, 2, 1, 15, 9, 15),
            Block.box(6, 9, 0, 16, 11, 16),
            Block.box(1, 9, 1, 8, 14, 15),
            Block.box(0, 14, 0, 10, 16, 16)
    );

    public static final DeferredBlock<ModelShapedBlock> WRITING_STATION = BLOCKS.registerBlock("writing_station",
            properties -> new ModelShapedBlock(properties, WRITING_STATION_SHAPE),
            properties -> properties.strength(2.5F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<EnchantmentStationBlock> ENCHANTMENT_STATION = BLOCKS.registerBlock("enhancement_station",
            EnchantmentStationBlock::new,
            properties -> properties.strength(2.5F).sound(SoundType.WOOD).noOcclusion());

    /** Placeable books: up to 4 per block, breaks instantly (even by hand, like a flower), drops plain books. */
    public static final DeferredBlock<BookPileBlock> BOOK_PILE = BLOCKS.registerBlock("book_pile", BookPileBlock::new,
            properties -> properties.instabreak().sound(SoundType.CHISELED_BOOKSHELF).noOcclusion().pushReaction(PushReaction.DESTROY));

    private LibrarianBlocks() {
    }
}
