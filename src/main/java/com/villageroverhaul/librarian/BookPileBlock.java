package com.villageroverhaul.librarian;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Placeable book pile (Librarian, 2026-09-24): up to 4 books on one block, stacked the way Vanilla
 * stacks candles and pink petals - clicking the pile again with a book pile item "replaces" the block
 * with itself one book higher (canBeReplaced + getStateForPlacement). BOOKS lives in the block state,
 * so it is saved and synced like any Vanilla state. The look is a multipart blockstate: part n is drawn
 * from n books on, turned by FACING. Breaking drops that many plain books (loot table), so the trade
 * that makes piles out of books can be undone.
 */
public class BookPileBlock extends Block {

    public static final MapCodec<BookPileBlock> CODEC = simpleCodec(BookPileBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int MAX_BOOKS = 4;
    public static final IntegerProperty BOOKS = IntegerProperty.create("books", 1, MAX_BOOKS);

    /**
     * Each book's box for a north-facing pile, in 1/16 block - taken from book_pile.bbmodel, with the
     * model's 90-degree y rotations already applied (Vanilla's rotationZYX: x' = z, z' = -x around the origin).
     */
    private static final VoxelShape[] BOOK_BOXES = {
            Block.box(2, 0, 1, 9, 2, 7),
            Block.box(5, 0, 7, 11, 2, 14),
            Block.box(3, 2, 3, 9, 4, 10),
            Block.box(4, 4, 2, 10, 6, 9)
    };
    @SuppressWarnings("unchecked")
    private static final Map<Direction, VoxelShape>[] SHAPES = new Map[MAX_BOOKS];

    static {
        VoxelShape pile = Shapes.empty();
        for (int books = 1; books <= MAX_BOOKS; books++) {
            pile = Shapes.or(pile, BOOK_BOXES[books - 1]);
            SHAPES[books - 1] = Shapes.rotateHorizontal(pile);
        }
    }

    public BookPileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BOOKS, 1));
    }

    @Override
    protected MapCodec<BookPileBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BOOKS);
    }

    /** Vanilla's candle / petal rule: same item in hand, not sneaking, room for one more. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem()) && state.getValue(BOOKS) < MAX_BOOKS
                || super.canBeReplaced(state, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        return existing.is(this)
                ? existing.setValue(BOOKS, Math.min(MAX_BOOKS, existing.getValue(BOOKS) + 1))
                : defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BOOKS) - 1].get(state.getValue(FACING));
    }

    /** Needs something to lie on, like a candle. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return directionToNeighbour == Direction.DOWN && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
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
