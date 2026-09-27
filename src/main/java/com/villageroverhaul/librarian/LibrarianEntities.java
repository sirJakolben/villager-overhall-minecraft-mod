package com.villageroverhaul.librarian;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Librarian entities; sizes/tracking copied from Vanilla's experience_bottle and experience_orb entries. */
public final class LibrarianEntities {

    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(VillagerOverhaulMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownVillagerExperienceBottle>> VILLAGER_EXPERIENCE_BOTTLE =
            ENTITY_TYPES.registerEntityType("villager_experience_bottle", ThrownVillagerExperienceBottle::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

    public static final DeferredHolder<EntityType<?>, EntityType<VillagerExperienceOrb>> VILLAGER_EXPERIENCE_ORB =
            ENTITY_TYPES.registerEntityType("villager_experience_orb", VillagerExperienceOrb::new, MobCategory.MISC,
                    builder -> builder.noLootTable().sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(20));

    private LibrarianEntities() {
    }
}
