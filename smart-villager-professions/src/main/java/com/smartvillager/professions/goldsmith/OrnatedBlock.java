package com.smartvillager.professions.goldsmith;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * The ornated lapis and ornated redstone (2026-09-29/30): placed like a piston - FACING (all six directions) points at
 * the player, so the front (the model's up face) looks at whoever placed it and the back (down face) away. Placed on
 * the floor or the ceiling it is the other way round: the back faces the player.
 */
public class OrnatedBlock extends DirectionalBlock {

    public static final MapCodec<OrnatedBlock> CODEC = simpleCodec(OrnatedBlock::new);

    public OrnatedBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends OrnatedBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction looking = context.getNearestLookingDirection();
        // Vertical placement is the other way round (2026-09-30): looking down puts the back on top, looking up the front.
        return defaultBlockState().setValue(FACING, looking.getAxis() == Direction.Axis.Y ? looking : looking.getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
