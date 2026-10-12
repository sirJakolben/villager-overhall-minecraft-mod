package com.smartvillager.professions.librarian;

import com.mojang.serialization.MapCodec;
import com.smartvillager.professions.station.PassiveStationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Librarian's passive station: a 5-slot book container (EnchantmentStationBlockEntity) that opens the
 * hopper window on right-click. Pulse, comparator and pathing come from PassiveStationBlock. Stays a Librarian
 * job site in both POWERED states - ExtendPoiTypesEvent adds all states of the block (ProfessionStations).
 */
public class EnchantmentStationBlock extends PassiveStationBlock {

    public static final MapCodec<EnchantmentStationBlock> CODEC = simpleCodec(EnchantmentStationBlock::new);

    /** Half-height base, tall back wall, side wall, book row - the rotated open book has no hitbox. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 8, 16),
            Block.box(0, 8, 0, 16, 16, 2),
            Block.box(14, 8, 2, 16, 12, 16),
            Block.box(8, 8, 2, 14, 14, 8)
    );

    public EnchantmentStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<EnchantmentStationBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnchantmentStationBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
