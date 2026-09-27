package com.villageroverhaul.veteran;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class VeteranBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VillagerOverhaulMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeaponRackBlockEntity>> WEAPON_RACK =
            BLOCK_ENTITY_TYPES.register("weapon_rack",
                    () -> new BlockEntityType<>(WeaponRackBlockEntity::new, VeteranBlocks.WEAPON_RACK.get()));

    private VeteranBlockEntities() {
    }
}
