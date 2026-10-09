package com.villageroverhaul.traderework;

import com.villageroverhaul.traderework.mason.CrushingRule;
import com.villageroverhaul.traderework.goldsmith.SalvageRule;
import com.villageroverhaul.traderework.fisherman.BiomeFish;
import com.villageroverhaul.traderework.veteran.MobWeaponDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * The extension's own datapack registries. Trades and quests stay in the core's registries
 * (data/ModDataPackRegistries) - the extension only ships files for them, under data/vo_trade_rework/villageroverhaul/.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class TradeReworkRegistries {

    /** Crushing station rules (mason/CrushingRule), server-only - the client never needs them. */
    public static final ResourceKey<Registry<CrushingRule>> CRUSHING =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "crushing"));

    /** Salvage station rules (goldsmith/SalvageRule), server-only. */
    public static final ResourceKey<Registry<SalvageRule>> SALVAGE =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "salvage"));

    /** Mobs whose held weapon becomes a mob weapon, with its extras (veteran/MobWeaponDefinition) - synced: tooltips need the attack speed bonus. */
    public static final ResourceKey<Registry<MobWeaponDefinition>> MOB_WEAPON =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "mob_weapon"));

    /** The biome fish of each biome group (fisherman/BiomeFish), server-only - the client never needs them. */
    public static final ResourceKey<Registry<BiomeFish>> BIOME_FISH =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "biome_fish"));

    private TradeReworkRegistries() {
    }

    @SubscribeEvent
    static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CRUSHING, CrushingRule.CODEC);
        event.dataPackRegistry(SALVAGE, SalvageRule.CODEC);
        event.dataPackRegistry(BIOME_FISH, BiomeFish.CODEC);
        event.dataPackRegistry(MOB_WEAPON, MobWeaponDefinition.CODEC, MobWeaponDefinition.CODEC);
    }
}
