package com.smartvillager.professions.veteran;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Veteran's items: its challenge station and weapon rack (Veteran.md, 2026-09-27). */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class VeteranItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SmartVillagerProfessionsMod.MODID);

    public static final DeferredItem<BlockItem> TRAPPINGS_STATION = ITEMS.registerSimpleBlockItem(VeteranBlocks.TRAPPINGS_STATION);
    public static final DeferredItem<BlockItem> WEAPON_RACK = ITEMS.registerSimpleBlockItem(VeteranBlocks.WEAPON_RACK);

    private VeteranItems() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TRAPPINGS_STATION);
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(WEAPON_RACK);
        }
    }
}
