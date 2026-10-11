package com.smartvillager.professions.goldsmith;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;

/**
 * Reinforced glass with connected textures (2026-09-30): one property per direction says whether the neighbour there is
 * reinforced glass too. The multipart blockstate (blockstates/reinforced_glass.json) gives every face the texture for its
 * four in-plane neighbours - the frame opens towards each connected side (textures/block/reinforced_glass*.png). Faces
 * between two reinforced glass blocks are skipped like Vanilla glass (HalfTransparentBlock.skipRendering).
 */
public class ReinforcedGlassBlock extends TransparentBlock {

    public static final MapCodec<ReinforcedGlassBlock> CODEC = simpleCodec(ReinforcedGlassBlock::new);
    private static final Map<Direction, BooleanProperty> CONNECTED = PipeBlock.PROPERTY_BY_DIRECTION;

    public ReinforcedGlassBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTED.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<ReinforcedGlassBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTED.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(CONNECTED.get(direction), connects(context.getLevel(), context.getClickedPos().relative(direction)));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(CONNECTED.get(direction), neighborState.is(this));
    }

    private boolean connects(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(this);
    }
}
