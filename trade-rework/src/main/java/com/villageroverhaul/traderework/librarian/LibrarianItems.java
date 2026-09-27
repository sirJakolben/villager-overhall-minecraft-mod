package com.villageroverhaul.traderework.librarian;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Every item the Librarian introduces (Obsidian: Improved Villagers/Librarian.md, "Item-IDs und
 * Texturen"). Phase 1 of the Librarian plan: registered as plain items with no behavior (except the
 * throwable villager_experience_bottle and the special books, see SpecialBookItem), so they can
 * already appear in trades/quests and the creative inventory. writing_station and enhancement_station are
 * BlockItems of LibrarianBlocks (same ids as before, so no world data breaks), and so is book_pile (2026-09-24);
 * paper_pile follows the same way. Lore scrolls are one LoreScrollItem per origin and tier - trades/quests match plain item
 * ids, and loot tables can place each tier independently.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class LibrarianItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeReworkMod.MODID);

    // Enchantment glint and yellow (uncommon) name like Vanilla's Bottle o' Enchanting (Items.EXPERIENCE_BOTTLE).
    public static final DeferredItem<VillagerExperienceBottleItem> VILLAGER_EXPERIENCE_BOTTLE = ITEMS.registerItem("villager_experience_bottle", VillagerExperienceBottleItem::new,
            properties -> properties.rarity(Rarity.UNCOMMON).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    public static final DeferredItem<BlockItem> BOOK_PILE = ITEMS.registerSimpleBlockItem(LibrarianBlocks.BOOK_PILE);
    public static final DeferredItem<Item> PAPER_PILE = ITEMS.registerSimpleItem("paper_pile");
    public static final DeferredItem<BlockItem> WRITING_STATION = ITEMS.registerSimpleBlockItem(LibrarianBlocks.WRITING_STATION);
    public static final DeferredItem<BlockItem> ENCHANTMENT_STATION = ITEMS.registerSimpleBlockItem(LibrarianBlocks.ENCHANTMENT_STATION);
    // Enchantability 1 like Vanilla's book (Items.BOOK) - the table rolls the same levels, only from the book's tag.
    public static final DeferredItem<SpecialBookItem> WEAPON_BOOK = ITEMS.registerItem("weapon_book",
            properties -> new SpecialBookItem(properties, SpecialBookItem.WEAPON_BOOK_ENCHANTMENTS), properties -> properties.enchantable(1));
    public static final DeferredItem<SpecialBookItem> RANGED_BOOK = ITEMS.registerItem("ranged_book",
            properties -> new SpecialBookItem(properties, SpecialBookItem.RANGED_BOOK_ENCHANTMENTS), properties -> properties.enchantable(1));
    public static final DeferredItem<SpecialBookItem> TOOL_BOOK = ITEMS.registerItem("tool_book",
            properties -> new SpecialBookItem(properties, SpecialBookItem.TOOL_BOOK_ENCHANTMENTS), properties -> properties.enchantable(1));
    public static final DeferredItem<SpecialBookItem> ARMOR_BOOK = ITEMS.registerItem("armor_book",
            properties -> new SpecialBookItem(properties, SpecialBookItem.ARMOR_BOOK_ENCHANTMENTS), properties -> properties.enchantable(1));

    /** Lore scrolls (2026-09-26): four origins x three tiers, id "<origin>_lore_scroll_<tier>" - see LoreScrollItem. */
    public static final List<DeferredItem<LoreScrollItem>> LORE_SCROLLS = registerLoreScrolls();

    private LibrarianItems() {
    }

    private static List<DeferredItem<LoreScrollItem>> registerLoreScrolls() {
        List<DeferredItem<LoreScrollItem>> scrolls = new ArrayList<>();
        for (LoreScrollItem.Origin origin : LoreScrollItem.Origin.values()) {
            for (int tier = 1; tier <= LoreScrollItem.TIERS; tier++) {
                int scrollTier = tier;
                scrolls.add(ITEMS.registerItem(LoreScrollItem.id(origin, tier),
                        properties -> new LoreScrollItem(properties, origin, scrollTier), properties -> properties.stacksTo(1)));
            }
        }
        return List.copyOf(scrolls);
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            List.of(
                    VILLAGER_EXPERIENCE_BOTTLE,
                    BOOK_PILE, PAPER_PILE, WRITING_STATION, ENCHANTMENT_STATION,
                    WEAPON_BOOK, RANGED_BOOK, TOOL_BOOK, ARMOR_BOOK
            ).forEach(event::accept);
            LORE_SCROLLS.forEach(event::accept);
        }
    }
}
