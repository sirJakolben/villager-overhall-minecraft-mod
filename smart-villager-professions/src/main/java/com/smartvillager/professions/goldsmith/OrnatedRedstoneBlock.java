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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The ornated redstone (2026-09-30): decorative redstone wires. Placed like the ornated lapis (front = top texture, back =
 * bottom texture), but both textures turn towards the neighbouring ornated redstone in their face's plane, so the wire runs
 * on into the next block. The top texture's wire leads from its bottom edge to its left one, the bottom texture's from its
 * right edge to its top one; turned (clockwise quarter turns) so one end touches the neighbour while the free end of the top
 * texture points up or left and that of the bottom texture down or right. Without a neighbour the top texture lies as if
 * one were on its left, the bottom texture as if one were on its right. With several neighbours the first of left, right,
 * above, below counts. Left and above are as seen when looking at the face; on the top and bottom face north is above.
 * The blockstate picks one model per facing and pair of turns (models/block/ornated_redstone_<facing>_<front>_<back>.json).
 */
public class OrnatedRedstoneBlock extends OrnatedBlock {

    public static final MapCodec<OrnatedRedstoneBlock> CODEC = simpleCodec(OrnatedRedstoneBlock::new);
    /** Clockwise quarter turns of the front (top) texture. */
    public static final IntegerProperty FRONT_ROTATION = IntegerProperty.create("front_rotation", 0, 3);
    /** Clockwise quarter turns of the back (bottom) texture. */
    public static final IntegerProperty BACK_ROTATION = IntegerProperty.create("back_rotation", 0, 3);

    public OrnatedRedstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FRONT_ROTATION, 1).setValue(BACK_ROTATION, 1));
    }

    @Override
    protected MapCodec<OrnatedRedstoneBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRONT_ROTATION, BACK_ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withRotations(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return withRotations(state, level, pos);
    }

    private BlockState withRotations(BlockState state, BlockGetter level, BlockPos pos) {
        Direction front = state.getValue(FACING);
        return state.setValue(FRONT_ROTATION, turns(level, pos, front, true))
                .setValue(BACK_ROTATION, turns(level, pos, front.getOpposite(), false));
    }

    /** Quarter turns of the texture on the given face, from the first neighbour in its plane (left, right, above, below). */
    private int turns(BlockGetter level, BlockPos pos, Direction face, boolean top) {
        Direction left = textureLeft(face);
        Direction up = textureUp(face);
        if (connects(level, pos.relative(left))) {
            return top ? 1 : 2;
        }
        if (connects(level, pos.relative(left.getOpposite()))) {
            return top ? 2 : 1;
        }
        if (connects(level, pos.relative(up))) {
            return top ? 1 : 0;
        }
        if (connects(level, pos.relative(up.getOpposite()))) {
            return top ? 0 : 1;
        }
        return 1;
    }

    private boolean connects(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(this);
    }

    /** Where an unturned texture's top edge points on this face. */
    private static Direction textureUp(Direction face) {
        return switch (face) {
            case UP -> Direction.NORTH;
            case DOWN -> Direction.SOUTH;
            default -> Direction.UP;
        };
    }

    /** Where an unturned texture's left edge points on this face - the left of someone looking at it. */
    private static Direction textureLeft(Direction face) {
        return switch (face) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.NORTH;
            case UP, DOWN -> Direction.WEST;
        };
    }
}
