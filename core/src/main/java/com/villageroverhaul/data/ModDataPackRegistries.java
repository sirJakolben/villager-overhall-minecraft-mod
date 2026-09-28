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

    /** Every trade and quest of every section - data/<namespace>/villageroverhaul/exchange/*.json, synced to the client. */
    public static final ResourceKey<Registry<ItemExchange>> EXCHANGE =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "exchange"));

    private ModDataPackRegistries() {
    }

    @SubscribeEvent
    static void onNewRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(EXCHANGE, ItemExchange.CODEC, ItemExchange.CODEC);
    }
}
