package com.villageroverhaul.traderework.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Opens Vanilla's book window (the one a written book uses) for a lore fragment: a single page with
 * the fragment text, lore.villageroverhaul.<origin>_<tier>.<fragment> in the lang files.
 */
public final class LoreScrollScreens {

    private LoreScrollScreens() {
    }

    public static void open(String origin, int tier, int fragment) {
        Component page = Component.translatable("lore.vo_trade_rework." + origin + "_" + tier + "." + fragment);
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(List.of(page))));
    }
}
