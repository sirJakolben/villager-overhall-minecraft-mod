package com.villageroverhaul.traderework.fisherman;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Fisherman's entities: the bobber a villager casts (like Vanilla's fishing bobber: tiny, not saved, tracked close by). */
public final class FishermanEntities {

    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(TradeReworkMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<VillagerBobber>> VILLAGER_BOBBER =
            ENTITY_TYPES.registerEntityType("villager_bobber", VillagerBobber::new, MobCategory.MISC,
                    builder -> builder.noLootTable().noSave().noSummon().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(1));

    private FishermanEntities() {
    }
}
