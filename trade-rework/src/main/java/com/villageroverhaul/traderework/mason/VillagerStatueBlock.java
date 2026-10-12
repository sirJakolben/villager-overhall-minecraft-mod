package com.villageroverhaul.traderework.mason;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A grey stone villager on an armor stand's base plate (2026-09-28, Mason trade). One block per villager
 * type (desert, taiga, ...) - the Mason sells the one of his own biome (trade/VillagerStatueOutput). Who it
 * shows is rolled by the server on placing (StatueFigure, kept in FIGURE; PENDING until then): an adult
 * stands two blocks high like a door (HALF, only the lower half draws and drops), a child fits into the lower
 * block and leaves the upper one free. Placing always needs two free blocks - the figure is only known after
 * it. Faces the player in 16 directions like a mob head; the base plate stays square to the block. Drawn by
 * client/render/VillagerStatueRenderer from the villager's own model and textures, turned grey.
 *
 * Hitboxes are the villager's own (0.6 x 1.95 blocks, a child half of that) standing on the 12 x 1 x 12 px plate.
 */
public class VillagerStatueBlock extends BaseEntityBlock {

    public static final MapCodec<VillagerStatueBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.VILLAGER_TYPE).fieldOf("villager_type").forGetter(VillagerStatueBlock::villagerType),
            propertiesCodec()
    ).apply(instance, VillagerStatueBlock::new));

    /** 16 directions like a mob head (2026-09-28) - the statue can also stand diagonally. */
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    private static final int ROTATIONS = RotationSegment.getMaxSegmentIndex() + 1;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<StatueFigure> FIGURE = EnumProperty.create("figure", StatueFigure.class);

    /** The armor stand's base plate (ArmorStandModel "base_plate"): 12 x 1 x 12 px. */
    private static final VoxelShape PLATE = Block.box(2, 0, 2, 14, 1, 14);
    /** Adult villager, 0.6 x 1.95 blocks from the plate up: 9.6 px wide, 31.2 px high, 15 px of it in the lower block. */
    private static final VoxelShape ADULT_LOWER = Shapes.or(PLATE, Block.box(3.2, 1, 3.2, 12.8, 16, 12.8));
    private static final VoxelShape ADULT_UPPER = Block.box(3.2, 0, 3.2, 12.8, 16.2, 12.8);
    /** Child villager, 0.3 x 0.975 blocks: 4.8 px wide, 15.6 px high - pokes 0.6 px into the block above. */
    private static final VoxelShape BABY = Shapes.or(PLATE, Block.box(5.6, 1, 5.6, 10.4, 16.6, 10.4));

    private final ResourceKey<VillagerType> villagerType;

    public VillagerStatueBlock(ResourceKey<VillagerType> villagerType, Properties properties) {
        super(properties);
        this.villagerType = villagerType;
        registerDefaultState(stateDefinition.any()
                .setValue(ROTATION, 0)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FIGURE, StatueFigure.PENDING));
    }

    public ResourceKey<VillagerType> villagerType() {
        return villagerType;
    }

    @Override
    protected MapCodec<VillagerStatueBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, HALF, FIGURE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new VillagerStatueBlockEntity(pos, state) : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        StatueFigure figure = state.getValue(FIGURE);
        if (figure == StatueFigure.PENDING) {
            return PLATE;
        }
        if (figure.isBaby()) {
            return BABY;
        }
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? ADULT_LOWER : ADULT_UPPER;
    }

    /** Placed not rolled yet (PENDING) - the server rolls in onPlace, so both sides place the same state. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
                // The player's view turned around: the statue looks back at them, in 22.5 degree steps.
                .setValue(ROTATION, RotationSegment.convertToSegment(context.getRotation() + 180.0F));
    }

    /**
     * Server only, for every way a statue gets set (player, /setblock, structures): rolls the figure of a new
     * statue and gives an adult its upper half. Without room above (only possible outside player placing) it
     * becomes a child. The new state reaches the client as one block update - it never shows a figure of its own.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide() || state.getValue(HALF) != DoubleBlockHalf.LOWER || state.getValue(FIGURE) != StatueFigure.PENDING) {
            return;
        }
        StatueFigure figure = StatueFigure.random(level.getRandom());
        BlockPos above = pos.above();
        boolean roomAbove = above.getY() <= level.getMaxY() && level.getBlockState(above).canBeReplaced();
        if (!figure.isBaby() && !roomAbove) {
            figure = StatueFigure.BABY;
        }
        BlockState rolled = state.setValue(FIGURE, figure);
        level.setBlock(pos, rolled, Block.UPDATE_ALL);
        if (!figure.isBaby()) {
            level.setBlock(above, rolled.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    /** Like a door: the two halves of an adult only exist together; a child stands alone. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean towardsOtherHalf = directionToNeighbour == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        if (towardsOtherHalf && standsAsPair(state.getValue(FIGURE))
                && !(neighbourState.is(this) && neighbourState.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    /** Only a rolled adult has two halves - a child and a not yet rolled statue stand alone. */
    private static boolean standsAsPair(StatueFigure figure) {
        return !figure.isBaby() && figure != StatueFigure.PENDING;
    }

    /**
     * Like a door: breaking the upper half in creative (or without a pickaxe) removes the lower one without a
     * drop; otherwise the lower half drops the statue when it falls away (its loot table only drops on "lower").
     */
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

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), ROTATIONS));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), ROTATIONS));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
