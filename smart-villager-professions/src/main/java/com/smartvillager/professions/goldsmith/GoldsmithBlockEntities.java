package com.smartvillager.professions.goldsmith;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GoldsmithBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SmartVillagerProfessionsMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SalvageStationBlockEntity>> SALVAGE_STATION =
            BLOCK_ENTITY_TYPES.register("salvage_station",
                    () -> new BlockEntityType<>(SalvageStationBlockEntity::new, GoldsmithBlocks.SALVAGE_STATION.get()));

    private GoldsmithBlockEntities() {
    }
}
