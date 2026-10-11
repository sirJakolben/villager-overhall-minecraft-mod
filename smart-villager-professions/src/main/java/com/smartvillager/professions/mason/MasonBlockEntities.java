package com.smartvillager.professions.mason;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.stream.Collectors;

public final class MasonBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SmartVillagerProfessionsMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrushingStationBlockEntity>> CRUSHING_STATION =
            BLOCK_ENTITY_TYPES.register("crushing_station",
                    () -> new BlockEntityType<>(CrushingStationBlockEntity::new, MasonBlocks.CRUSHING_STATION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VillagerStatueBlockEntity>> VILLAGER_STATUE =
            BLOCK_ENTITY_TYPES.register("villager_statue", () -> new BlockEntityType<>(VillagerStatueBlockEntity::new,
                    MasonBlocks.VILLAGER_STATUES.values().stream().map(DeferredBlock::get).collect(Collectors.toSet())));

    private MasonBlockEntities() {
    }
}
