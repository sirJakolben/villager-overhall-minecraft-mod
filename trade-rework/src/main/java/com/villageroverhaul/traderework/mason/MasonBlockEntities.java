package com.villageroverhaul.traderework.mason;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MasonBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TradeReworkMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrushingStationBlockEntity>> CRUSHING_STATION =
            BLOCK_ENTITY_TYPES.register("crushing_station",
                    () -> new BlockEntityType<>(CrushingStationBlockEntity::new, MasonBlocks.CRUSHING_STATION.get()));

    private MasonBlockEntities() {
    }
}
