package com.villageroverhaul.traderework.salvager;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SalvagerBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TradeReworkMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmeltingStationBlockEntity>> SMELTING_STATION =
            BLOCK_ENTITY_TYPES.register("smelting_station",
                    () -> new BlockEntityType<>(SmeltingStationBlockEntity::new, SalvagerBlocks.SMELTING_STATION.get()));

    private SalvagerBlockEntities() {
    }
}
