package com.smartvillager.professions;

import com.smartvillager.professions.mapping.MapPool;
import com.smartvillager.professions.mason.CrushingRule;
import com.smartvillager.professions.goldsmith.SalvageRule;
import com.smartvillager.professions.fisherman.BiomeFish;
import com.smartvillager.professions.veteran.MobWeaponDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The extension's own datapack registries. Trades and quests stay in the core's registries
 * (data/ModDataPackRegistries) - the extension only ships files for them, under data/smartvillager_professions/smartvillager_progress/.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class ProfessionsRegistries {

    /** Crushing station rules (mason/CrushingRule), server-only - the client never needs them. */
    public static final ResourceKey<Registry<CrushingRule>> CRUSHING =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, "crushing"));

    /** Salvage station rules (goldsmith/SalvageRule), server-only. */
    public static final ResourceKey<Registry<SalvageRule>> SALVAGE =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, "salvage"));

    /** Mobs whose held weapon becomes a mob weapon, with its extras (veteran/MobWeaponDefinition) - synced: tooltips need the attack speed bonus. */
    public static final ResourceKey<Registry<MobWeaponDefinition>> MOB_WEAPON =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, "mob_weapon"));

    /** The biome fish of each biome group (fisherman/BiomeFish), server-only - the client never needs them. */
    public static final ResourceKey<Registry<BiomeFish>> BIOME_FISH =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, "biome_fish"));

    /** Which structures a passive station's maps may point to (mapping/MapPool), server-only. */
    public static final ResourceKey<Registry<MapPool>> MAP_POOL =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, "map_pool"));

    private ProfessionsRegistries() {
    }

    @SubscribeEvent
    static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CRUSHING, CrushingRule.CODEC);
        event.dataPackRegistry(SALVAGE, SalvageRule.CODEC);
        event.dataPackRegistry(BIOME_FISH, BiomeFish.CODEC);
        event.dataPackRegistry(MAP_POOL, MapPool.CODEC);
        event.dataPackRegistry(MOB_WEAPON, MobWeaponDefinition.CODEC, MobWeaponDefinition.CODEC);
    }
}
