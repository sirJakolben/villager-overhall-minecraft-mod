package com.villageroverhaul.traderework.basket;

import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.farmer.FarmerBlocks;
import com.villageroverhaul.traderework.fisherman.FishermanBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One block entity type for both baskets - the harvest basket (Farmer) and the fish basket (Fisherman). */
public final class BasketBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TradeReworkMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BasketStationBlockEntity>> BASKET_STATION =
            BLOCK_ENTITY_TYPES.register("basket_station",
                    () -> new BlockEntityType<>(BasketStationBlockEntity::new, FarmerBlocks.HARVEST_BASKET_STATION.get(), FishermanBlocks.FISH_BASKET_STATION.get()));

    private BasketBlockEntities() {
    }
}
