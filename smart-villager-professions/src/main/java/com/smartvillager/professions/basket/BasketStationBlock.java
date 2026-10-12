package com.smartvillager.professions.basket;

import com.mojang.serialization.MapCodec;
import com.smartvillager.professions.station.FacingPassiveStationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The passive station of the Farmer (harvest basket) and the Fisherman (fish basket) - one block class for both
 * (Obsidian Farmer.md / Fisherman.md, 2026-09-30). A 5-slot container (BasketStationBlockEntity) that opens Vanilla's hopper
 * window on right-click, like the Librarian's enhancement station; what the villager makes of his counters lands in it
 * (passive/HarvestWork, passive/FishingWork). Pulse, comparator and pathing come from PassiveStationBlock. A job site of its
 * profession in every state (ProfessionStations).
 *
 * FILL (0-5) is the number of occupied slots; the models show the basket that full (models/block/*_basket_station[_1-5].json).
 * The shape is a 14 x 14 x 9 px box, centered.
 */
public class BasketStationBlock extends FacingPassiveStationBlock {

    public static final MapCodec<BasketStationBlock> CODEC = simpleCodec(BasketStationBlock::new);
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, BasketStationBlockEntity.SLOTS);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 9, 15);

    public BasketStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FILL, 0));
    }

    @Override
    protected MapCodec<BasketStationBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FILL);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BasketStationBlockEntity(pos, state);
    }
}
