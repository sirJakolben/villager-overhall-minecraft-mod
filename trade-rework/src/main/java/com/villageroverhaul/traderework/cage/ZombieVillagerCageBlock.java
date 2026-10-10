package com.villageroverhaul.traderework.cage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Zombie Villager Cage: one block wide, two high like a door (the zombie villager is 1.95 blocks), so exactly one fits. An Iron Golem carries a
 * zombie villager in and the cage keeps it - the cage holds it as saved data only (ZombieVillagerCageBlockEntity), never as a living mob, so it
 * cannot attack, burn or wander. Breaking the cage lets it out again.
 *
 * Right-click cures it, the Vanilla way: a potion of weakness first, then a golden apple. Only the lower half has the block entity; OCCUPIED is
 * kept equal on both halves. The lower half of a free cage is a point of interest (Cages.CAGE_POI), which is how the golems find it.
 */
public class ZombieVillagerCageBlock extends BaseEntityBlock {

    public static final MapCodec<ZombieVillagerCageBlock> CODEC = simpleCodec(ZombieVillagerCageBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public ZombieVillagerCageBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(OCCUPIED, false));
    }

    @Override
    protected MapCodec<ZombieVillagerCageBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, OCCUPIED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ZombieVillagerCageBlockEntity(pos, state) : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState();
    }

    /** Like a door: the upper half is placed with the lower. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /** Like a door: the two halves only exist together. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean towardsOtherHalf = directionToNeighbour == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        if (towardsOtherHalf && !(neighbourState.is(this) && neighbourState.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    /** Like a door: breaking the upper half without the right tool or in creative removes the lower one without a drop. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.UPPER
                && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            BlockPos lowerPos = pos.below();
            BlockState lower = level.getBlockState(lowerPos);
            if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, lowerPos, Block.getId(lower));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Potion of weakness, then golden apple - on either half. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (level.getBlockEntity(lowerPos) instanceof ZombieVillagerCageBlockEntity cage) {
            return cage.interact(stack, player, hand);
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** The scheduled end of a cure (ZombieVillagerCageBlockEntity.startCure). */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER && level.getBlockEntity(pos) instanceof ZombieVillagerCageBlockEntity cage) {
            cage.finishCureIfDue(level);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    /** Sets OCCUPIED on both halves; the lower half is the one given. */
    static void setOccupied(Level level, BlockPos lowerPos, boolean occupied) {
        for (BlockPos pos : new BlockPos[] {lowerPos, lowerPos.above()}) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ZombieVillagerCageBlock && state.getValue(OCCUPIED) != occupied) {
                level.setBlock(pos, state.setValue(OCCUPIED, occupied), Block.UPDATE_ALL);
            }
        }
    }
}
