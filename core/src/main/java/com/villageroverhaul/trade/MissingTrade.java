package com.villageroverhaul.trade;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Stand-in for a trade that isn't designed yet (2026-09-27): a trade file whose output is this item (and
 * max uses 0) shows as a completely empty, disabled row - no cost, no arrow, no result, no text
 * (client/ui/VillagerScreen.isEmptyRow) - so in game you see which ranks are still empty. Nothing can be bought; delete the file (trade/*_todo_*.json)
 * once the real trade exists. Not in any creative tab.
 */
public final class MissingTrade {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VillagerOverhaulMod.MODID);

    public static final DeferredItem<Item> ITEM = ITEMS.registerSimpleItem("missing_trade");

    private MissingTrade() {
    }
}
