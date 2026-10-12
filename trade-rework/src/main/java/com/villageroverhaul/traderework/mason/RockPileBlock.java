package com.villageroverhaul.traderework.mason;

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
 * Decorative rock pile (Mason, 2026-09-26): up to 4 rock groups on one block, stacked the same way as the Librarian's
 * book pile (librarian/BookPileBlock) - clicking the pile again with a rock pile adds a group. Breaking it with
 * a pickaxe drops one cobblestone per group (loot table) - the trade made it from cobblestone. RockPathBlock reuses the stacking with its own boxes and footing.
 */
public class RockPileBlock extends Block {

    public static final MapCodec<RockPileBlock> CODEC = simpleCodec(RockPileBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int MAX_ROCKS = 4;
    public static final IntegerProperty ROCKS = IntegerProperty.create("rocks", 1, MAX_ROCKS);

    /**
     * What each added rock group brings for a north-facing block, in 1/16 block - the four groups of
     * rock_pile.bbmodel (assets/Mason/rock_pile), one model file per group (models/block/rock_pile_1..4.json).
     */
    private static final VoxelShape[] ROCK_BOXES = {
            Block.box(5, 0, 2, 9, 3, 7),
            Shapes.or(Block.box(8, 0, 0, 11, 2, 3), Block.box(1, 0, 9, 3, 2, 11)),
            Block.box(3, 0, 10, 9, 5, 16),
            Block.box(8, 0, 4, 16, 8, 13)
    };
    @SuppressWarnings("unchecked")
    private static final Map<Direction, VoxelShape>[] SHAPES = new Map[MAX_ROCKS];

    static {
        VoxelShape pile = Shapes.empty();
        for (int rocks = 1; rocks <= MAX_ROCKS; rocks++) {
            pile = Shapes.or(pile, ROCK_BOXES[rocks - 1]);
            SHAPES[rocks - 1] = Shapes.rotateHorizontal(pile);
        }
    }

    public RockPileBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROCKS, 1));
    }

    @Override
    protected MapCodec<? extends RockPileBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROCKS);
    }

    /** Vanilla's candle / petal rule: same item in hand, not sneaking, room for one more. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem()) && state.getValue(ROCKS) < MAX_ROCKS
                || super.canBeReplaced(state, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        return existing.is(this)
                ? existing.setValue(ROCKS, Math.min(MAX_ROCKS, existing.getValue(ROCKS) + 1))
                : defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(ROCKS) - 1].get(state.getValue(FACING));
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
