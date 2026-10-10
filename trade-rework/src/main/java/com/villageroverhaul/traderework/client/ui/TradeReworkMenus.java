package com.villageroverhaul.traderework.client.ui;

import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.librarian.EnchantmentStationMenu;
import com.villageroverhaul.traderework.station.ThreeInThreeOutMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TradeReworkMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, TradeReworkMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<EnchantmentStationMenu>> ENCHANTMENT_STATION_MENU = MENU_TYPES.register(
            "enhancement_station",
            () -> new MenuType<>(EnchantmentStationMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> CRUSHING_STATION_MENU = MENU_TYPES.register(
            "crushing_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(TradeReworkMenus.CRUSHING_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> REPAIR_STATION_MENU = MENU_TYPES.register(
            "repair_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(TradeReworkMenus.REPAIR_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> SALVAGE_STATION_MENU = MENU_TYPES.register(
            "salvage_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(TradeReworkMenus.SALVAGE_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> ADVENTURE_STATION_MENU = MENU_TYPES.register(
            "adventure_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(TradeReworkMenus.ADVENTURE_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> CHALLENGE_STATION_MENU = MENU_TYPES.register(
            "challenge_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(TradeReworkMenus.CHALLENGE_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    private TradeReworkMenus() {
    }
}
