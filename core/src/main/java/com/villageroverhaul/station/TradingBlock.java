package com.villageroverhaul.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Trading Block. Right-clicking it - with anything in hand, including an emerald when no
 * villager is selected - calls its villager for a short visit (TradingBlockCall.callByClick) and swings
 * the player's hand like clicking a villager does. A right-click with an emerald while a villager is
 * selected never gets here on the server: EmeraldClaimTool cancels it and assigns the villager instead,
 * while the client still runs this and swings the hand. Like any usable block, sneaking lets the player
 * place blocks against it instead.
 */
public class TradingBlock extends Block {

    public static final MapCodec<TradingBlock> CODEC = simpleCodec(TradingBlock::new);

    /** Bottom and top plate, two side walls, and the crossed staff in the back - from trading_block.bbmodel. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(0, 14, 0, 16, 16, 16),
            Block.box(0, 2, 0, 2, 14, 16),
            Block.box(14, 2, 0, 16, 14, 16),
            Block.box(6.5, 2, 1, 9.5, 14, 3),
            Block.box(2, 6.5, 2, 14, 9.5, 4)
    );

    public TradingBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<TradingBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel) {
            TradingBlockCall.callByClick(serverLevel, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
