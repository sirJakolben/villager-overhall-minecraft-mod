package com.villageroverhaul.data;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.mason.CrushingRule;
import com.villageroverhaul.salvager.SalvageRule;
import com.villageroverhaul.veteran.MobWeaponDefinition;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class ModDataPackRegistries {

    public static final ResourceKey<Registry<ItemExchange>> TRADE =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "trade"));

    public static final ResourceKey<Registry<ItemExchange>> QUEST =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "quest"));

    public static final ResourceKey<Registry<PassiveAbilityDefinition>> PASSIVE =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "passive"));

    /** Crushing station rules (mason/CrushingRule), server-only - the client never needs them. */
    public static final ResourceKey<Registry<CrushingRule>> CRUSHING =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "crushing"));

    /** Smelting station rules (salvager/SalvageRule), server-only. */
    public static final ResourceKey<Registry<SalvageRule>> SALVAGE =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "salvage"));

    /** Mobs whose held weapon becomes a mob weapon, with its extras (veteran/MobWeaponDefinition) - synced: tooltips need the attack speed bonus. */
    public static final ResourceKey<Registry<MobWeaponDefinition>> MOB_WEAPON =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "mob_weapon"));

    private ModDataPackRegistries() {
    }

    @SubscribeEvent
    static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(TRADE, ItemExchange.CODEC, ItemExchange.CODEC);
        event.dataPackRegistry(QUEST, ItemExchange.CODEC, ItemExchange.CODEC);
        event.dataPackRegistry(PASSIVE, PassiveAbilityDefinition.CODEC, PassiveAbilityDefinition.CODEC);
        event.dataPackRegistry(CRUSHING, CrushingRule.CODEC);
        event.dataPackRegistry(SALVAGE, SalvageRule.CODEC);
        event.dataPackRegistry(MOB_WEAPON, MobWeaponDefinition.CODEC, MobWeaponDefinition.CODEC);
    }
}
