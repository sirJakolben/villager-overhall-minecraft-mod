package com.villageroverhaul.traderework.mason;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.VillagerType;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A grey stone villager on an armor stand's base plate (2026-09-28, Mason trade). One block per villager
 * type (desert, taiga, ...) - the Mason sells the one of his own biome (trade/VillagerStatueOutput). Who it
 * shows is rolled on placing (StatueFigure, kept in FIGURE): an adult stands two blocks high like a door
 * (HALF, only the lower half draws and drops), a child fits into the lower block and leaves the upper one
 * free. Placing always needs two free blocks - the figure is only known after it. Faces the player like the
 * copper golem statue. Drawn by client/render/VillagerStatueRenderer from the villager's own model and
 * textures, turned grey.
 *
 * Hitboxes are the villager's own (0.6 x 1.95 blocks, a child half of that) standing on the 12 x 1 x 12 px plate.
 */
public class VillagerStatueBlock extends BaseEntityBlock {

    public static final MapCodec<VillagerStatueBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.VILLAGER_TYPE).fieldOf("villager_type").forGetter(VillagerStatueBlock::villagerType),
            propertiesCodec()
    ).apply(instance, VillagerStatueBlock::new));

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
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
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FIGURE, StatueFigure.NONE));
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
        builder.add(FACING, HALF, FIGURE);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new VillagerStatueBlockEntity(pos, state) : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(FIGURE).isBaby()) {
            return BABY;
        }
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? ADULT_LOWER : ADULT_UPPER;
    }

    /** Rolls the figure; the server's roll wins, the client's guess is replaced by the block update. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxY() || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FIGURE, StatueFigure.random(level.getRandom()));
    }

    /** An adult gets its upper half, a child doesn't. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        if (!state.getValue(FIGURE).isBaby()) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    /** Like a door: the two halves of an adult only exist together; a child stands alone. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean towardsOtherHalf = directionToNeighbour == (half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN);
        if (towardsOtherHalf && !state.getValue(FIGURE).isBaby()
                && !(neighbourState.is(this) && neighbourState.getValue(HALF) != half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
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
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
