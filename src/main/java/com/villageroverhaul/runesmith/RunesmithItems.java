package com.villageroverhaul.runesmith;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Runesmith's items: its two stations and the upgrade template (Runesmith.md). One template for every
 * tier (2026-09-27, before: four): it is a plain item - what it does comes from Vanilla smithing_transform
 * recipes in data/villageroverhaul/recipe/*_smithing.json, exactly like the netherite upgrade: template +
 * item + one BLOCK of the next tier's material gives the next tier, keeping enchantments, damage, name,
 * trims and the mob-weapon mark. Used up on the smithing table; only the Runesmith sells it.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class RunesmithItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VillagerOverhaulMod.MODID);

    public static final DeferredItem<BlockItem> UPGRADE_STATION = ITEMS.registerSimpleBlockItem(RunesmithBlocks.UPGRADE_STATION);
    public static final DeferredItem<BlockItem> REPAIR_STATION = ITEMS.registerSimpleBlockItem(RunesmithBlocks.REPAIR_STATION);
    public static final DeferredItem<Item> UPGRADE_TEMPLATE = ITEMS.registerSimpleItem("upgrade_smithing_template");

    private RunesmithItems() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            List.of(UPGRADE_STATION, REPAIR_STATION).forEach(event::accept);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(UPGRADE_TEMPLATE);
        }
    }
}
