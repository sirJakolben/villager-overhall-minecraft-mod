package com.smartvillager.professions.client.ui;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.librarian.EnchantmentStationMenu;
import com.smartvillager.professions.station.ThreeInThreeOutMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProfessionsMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, SmartVillagerProfessionsMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<EnchantmentStationMenu>> ENCHANTMENT_STATION_MENU = MENU_TYPES.register(
            "enhancement_station",
            () -> new MenuType<>(EnchantmentStationMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> CRUSHING_STATION_MENU = MENU_TYPES.register(
            "crushing_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(ProfessionsMenus.CRUSHING_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> REPAIR_STATION_MENU = MENU_TYPES.register(
            "repair_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(ProfessionsMenus.REPAIR_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> SALVAGE_STATION_MENU = MENU_TYPES.register(
            "salvage_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(ProfessionsMenus.SALVAGE_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> ADVENTURE_STATION_MENU = MENU_TYPES.register(
            "adventure_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(ProfessionsMenus.ADVENTURE_STATION_MENU.get(), id, inventory, 3, 3), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> CHALLENGE_STATION_MENU = MENU_TYPES.register(
            "challenge_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(ProfessionsMenus.CHALLENGE_STATION_MENU.get(), id, inventory, 1, 4), FeatureFlags.VANILLA_SET)
    );

    private ProfessionsMenus() {
    }
}
