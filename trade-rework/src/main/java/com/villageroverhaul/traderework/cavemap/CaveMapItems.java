package com.villageroverhaul.traderework.cavemap;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Cartographer's cave maps (2026-10-10, his Masteries 3 and 4): empty Cave Map / Nether Map (EmptyCaveMapItem) that become a filled
 * one (CaveMapItem) on right-click inside a layer, and the layer component that binds a filled map to its height band.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class CaveMapItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeReworkMod.MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TradeReworkMod.MODID);

    /** The layer a filled cave map belongs to - set when it is made, never changed. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CaveLayer>> CAVE_LAYER =
            DATA_COMPONENTS.registerComponentType("cave_layer",
                    builder -> builder.persistent(CaveLayer.CODEC).networkSynchronized(CaveLayer.STREAM_CODEC));

    public static final DeferredItem<EmptyCaveMapItem> CAVE_MAP =
            ITEMS.registerItem("cave_map", properties -> new EmptyCaveMapItem(properties, CaveMapKind.CAVE));
    public static final DeferredItem<EmptyCaveMapItem> NETHER_MAP =
            ITEMS.registerItem("nether_map", properties -> new EmptyCaveMapItem(properties, CaveMapKind.NETHER));
    public static final DeferredItem<CaveMapItem> FILLED_CAVE_MAP =
            ITEMS.registerItem("filled_cave_map", properties -> new CaveMapItem(properties, CaveMapKind.CAVE));
    public static final DeferredItem<CaveMapItem> FILLED_NETHER_MAP =
            ITEMS.registerItem("filled_nether_map", properties -> new CaveMapItem(properties, CaveMapKind.NETHER));

    private CaveMapItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
    }

    public static DeferredItem<EmptyCaveMapItem> emptyOf(CaveMapKind kind) {
        return kind == CaveMapKind.CAVE ? CAVE_MAP : NETHER_MAP;
    }

    public static DeferredItem<CaveMapItem> filledOf(CaveMapKind kind) {
        return kind == CaveMapKind.CAVE ? FILLED_CAVE_MAP : FILLED_NETHER_MAP;
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CAVE_MAP);
            event.accept(NETHER_MAP);
        }
    }
}
