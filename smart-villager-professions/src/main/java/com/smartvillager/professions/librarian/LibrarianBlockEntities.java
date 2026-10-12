package com.smartvillager.professions.librarian;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LibrarianBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SmartVillagerProfessionsMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnchantmentStationBlockEntity>> ENCHANTMENT_STATION =
            BLOCK_ENTITY_TYPES.register("enhancement_station",
                    () -> new BlockEntityType<>(EnchantmentStationBlockEntity::new, LibrarianBlocks.ENCHANTMENT_STATION.get()));

    private LibrarianBlockEntities() {
    }
}
