package com.smartvillager.professions.fisherman;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.boat.CannonballEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Fisherman's entities: the bobber a villager casts (like Vanilla's fishing bobber: tiny, not saved, tracked close by). */
public final class FishermanEntities {

    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(SmartVillagerProfessionsMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<VillagerBobber>> VILLAGER_BOBBER =
            ENTITY_TYPES.registerEntityType("villager_bobber", VillagerBobber::new, MobCategory.MISC,
                    builder -> builder.noLootTable().noSave().noSummon().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(1));

    /** The boat cannon's ball: sized and tracked like a thrown item. */
    public static final DeferredHolder<EntityType<?>, EntityType<CannonballEntity>> CANNONBALL =
            ENTITY_TYPES.registerEntityType("cannonball", CannonballEntity::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

    private FishermanEntities() {
    }
}
