package com.villageroverhaul.data;

import com.villageroverhaul.VillagerOverhaulMod;
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


    private ModDataPackRegistries() {
    }

    @SubscribeEvent
    static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(TRADE, ItemExchange.CODEC, ItemExchange.CODEC);
        event.dataPackRegistry(QUEST, ItemExchange.CODEC, ItemExchange.CODEC);
        event.dataPackRegistry(PASSIVE, PassiveAbilityDefinition.CODEC, PassiveAbilityDefinition.CODEC);
    }
}
