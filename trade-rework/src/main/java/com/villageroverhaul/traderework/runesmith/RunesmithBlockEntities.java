package com.villageroverhaul.traderework.runesmith;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RunesmithBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TradeReworkMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RepairStationBlockEntity>> REPAIR_STATION =
            BLOCK_ENTITY_TYPES.register("repair_station",
                    () -> new BlockEntityType<>(RepairStationBlockEntity::new, RunesmithBlocks.REPAIR_STATION.get()));

    private RunesmithBlockEntities() {
    }
}
