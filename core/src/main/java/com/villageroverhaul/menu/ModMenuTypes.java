package com.villageroverhaul.menu;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.core.registries.Registries;
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

    private ModMenuTypes() {
    }
}
