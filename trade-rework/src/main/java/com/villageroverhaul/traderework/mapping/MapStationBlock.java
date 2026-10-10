package com.villageroverhaul.traderework.mapping;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.traderework.station.PassiveStationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The passive station of the Cartographer (adventure station, adventure maps) and of the Veteran (challenge station, challenge maps) -
 * one block class for both, the pool decides (2026-10-10). A 3 + 3 slot container (MapStationBlockEntity) that opens its window on
 * right-click; pulse, comparator and pathing come from PassiveStationBlock. Stays a job site of its profession in all states
 * (ProfessionStations). Placeholder cube until the models exist.
 */
public class MapStationBlock extends PassiveStationBlock {

    public static final MapCodec<MapStationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            MapPool.Kind.CODEC.fieldOf("pool").forGetter(MapStationBlock::kind)
    ).apply(instance, MapStationBlock::new));

    private final MapPool.Kind kind;

    public MapStationBlock(Properties properties, MapPool.Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public MapPool.Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<MapStationBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MapStationBlockEntity(MappingBlocks.blockEntityType(kind), pos, state, kind);
    }
}
