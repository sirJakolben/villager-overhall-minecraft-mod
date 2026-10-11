package com.smartvillager.professions.fisherman;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Fisherman's items (Obsidian Fisherman.md, 2026-09-30): the eight biome fish, one per biome group (their biomes: data/smartvillager_professions/smartvillager_professions/biome_fish/,
 * tags/worldgen/biome/fish_*.json). The names are working titles - the Obsidian note only fixes the emerald fish (mountains); what each other fish
 * is good for is open (the fish dishes moved to the Butcher, 2026-09-30). So for now they are plain items; the item tag
 * smartvillager_professions:biome_fish lists them for trades. Textures still point at Vanilla's fish ("Fehlende Texturen und Modelle.md").
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class FishermanItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SmartVillagerProfessionsMod.MODID);

    /** Mountains - sold to the Fisherman for emeralds (a permanent quest, still to be built). */
    public static final DeferredItem<Item> EMERALD_FISH = ITEMS.registerSimpleItem("emerald_fish");
    public static final DeferredItem<Item> DUNE_FISH = ITEMS.registerSimpleItem("dune_fish");
    public static final DeferredItem<Item> SUNSET_FISH = ITEMS.registerSimpleItem("sunset_fish");
    public static final DeferredItem<Item> ABYSS_FISH = ITEMS.registerSimpleItem("abyss_fish");
    public static final DeferredItem<Item> NEEDLE_FISH = ITEMS.registerSimpleItem("needle_fish");
    public static final DeferredItem<Item> FROST_FISH = ITEMS.registerSimpleItem("frost_fish");
    public static final DeferredItem<Item> CANOPY_FISH = ITEMS.registerSimpleItem("canopy_fish");
    public static final DeferredItem<Item> SPORE_FISH = ITEMS.registerSimpleItem("spore_fish");

    private FishermanItems() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            List.of(EMERALD_FISH, DUNE_FISH, SUNSET_FISH, ABYSS_FISH, NEEDLE_FISH, FROST_FISH, CANOPY_FISH, SPORE_FISH).forEach(event::accept);
        }
    }
}
