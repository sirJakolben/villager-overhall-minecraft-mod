package com.villageroverhaul.client.ui;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.librarian.EnchantmentStationMenu;
import com.villageroverhaul.station.ThreeInThreeOutMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, VillagerOverhaulMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<VillagerMenu>> VILLAGER_MENU = MENU_TYPES.register(
            "villager_menu",
            () -> IMenuTypeExtension.create(VillagerMenu::new)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<EnchantmentStationMenu>> ENCHANTMENT_STATION_MENU = MENU_TYPES.register(
            "enhancement_station",
            () -> new MenuType<>(EnchantmentStationMenu::new, FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> CRUSHING_STATION_MENU = MENU_TYPES.register(
            "crushing_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(ModMenuTypes.CRUSHING_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> REPAIR_STATION_MENU = MENU_TYPES.register(
            "repair_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(ModMenuTypes.REPAIR_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    public static final DeferredHolder<MenuType<?>, MenuType<ThreeInThreeOutMenu>> SMELTING_STATION_MENU = MENU_TYPES.register(
            "smelting_station",
            () -> new MenuType<>((id, inventory) -> new ThreeInThreeOutMenu(ModMenuTypes.SMELTING_STATION_MENU.get(), id, inventory), FeatureFlags.VANILLA_SET)
    );

    private ModMenuTypes() {
    }
}
