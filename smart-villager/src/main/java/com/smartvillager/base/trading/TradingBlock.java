package com.smartvillager.base.trading;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * The Trading Block. Right-clicking it - with anything in hand, including an emerald when no
 * villager is selected - calls its villager for a short visit (TradingBlockCall.callByClick) and swings
 * the player's hand like clicking a villager does. A right-click with an emerald while a villager is
 * selected never gets here on the server: EmeraldClaimTool cancels it and assigns the villager instead,
 * while the client still runs this and swings the hand. Like any usable block, sneaking lets the player
 * place blocks against it instead.
 *
 * Placed like a furnace (2026-09-29): FACING points at the player, so the model's north side - the one with
 * the staff - faces whoever placed it. The blockstate turns the model, the shape turns with it.
 */
public class TradingBlock extends Block {

    public static final MapCodec<TradingBlock> CODEC = simpleCodec(TradingBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /**
     * Facing north: bottom and top plate, two side walls, and the crossed staff on the north side - from
     * trading_block.bbmodel. The other directions are that shape turned.
     */
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(0, 2, 0, 2, 14, 16),
            Block.box(14, 2, 0, 16, 14, 16),
            Block.box(6.5, 2, 1, 9.5, 14, 3),
            Block.box(2, 6.5, 2, 14, 9.5, 4)
    ));

    public TradingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<TradingBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /**
     * Not walkable - like Vanilla's lectern. Vanilla counts every block without a full-cube collision shape as
     * passable, so villagers planned their paths straight through it, got stuck and never jumped over or went around.
     */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel) {
            TradingBlockCall.callByClick(serverLevel, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
