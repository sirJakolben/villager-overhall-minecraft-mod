package com.villageroverhaul.traderework.runesmith;

import com.mojang.serialization.MapCodec;
import com.villageroverhaul.traderework.station.PassiveStationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Runesmith's passive station: a 3 + 3 slot container (RepairStationBlockEntity) that opens its window
 * on right-click. Pulse, comparator and pathing come from PassiveStationBlock. Stays a Runesmith job site in
 * both POWERED states (ProfessionStations adds all states of the block).
 */
public class RepairStationBlock extends PassiveStationBlock {

    public static final MapCodec<RepairStationBlock> CODEC = simpleCodec(RepairStationBlock::new);

    public RepairStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<RepairStationBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RepairStationBlockEntity(pos, state);
    }

    /** Server only: the station plays the grindstone sound while its Runesmith fills the passive meter here. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel
                ? createTickerHelper(type, RunesmithBlockEntities.REPAIR_STATION.get(), (tickLevel, pos, tickState, station) -> station.serverTick((ServerLevel) tickLevel, pos))
                : null;
    }
}
