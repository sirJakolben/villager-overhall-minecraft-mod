package com.villageroverhaul.api;

import com.villageroverhaul.data.ExplorerMap;
import com.villageroverhaul.section.Sections;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Where an extension mod (the Trade Rework) plugs into the core. The core registers its own two sections through
 * the same registerSection (section/CoreSections). Every other hook is neutral until something sets it: no mob
 * weapons, no extra XP price, explorer-map offers sold out.
 *
 * Register from the extension's mod constructor. The extension declares ordering AFTER the core, so the core's
 * registries exist by then; registration is thread-safe because mod constructors may run in parallel. The three
 * setters hold one value each - the last extension to set one wins.
 */
public final class ExtensionHooks {

    /** An explorer-map trade's result; soldOut when nothing was found. */
    public record MapOutput(ItemStack stack, boolean soldOut) {
    }

    @FunctionalInterface
    public interface ExplorerMapBuilder {
        MapOutput outputFor(Villager villager, Identifier entryId, ExplorerMap spec);
    }

    private static volatile Supplier<@Nullable DataComponentType<EntityType<?>>> mobWeaponComponent = () -> null;
    private static volatile ToIntFunction<MerchantOffer> playerXpCost = offer -> 0;
    private static volatile ExplorerMapBuilder explorerMaps = (villager, entryId, spec) -> {
        ItemStack blank = new ItemStack(Items.MAP);
        blank.set(DataComponents.ITEM_NAME, Component.translatable(spec.name()));
        return new MapOutput(blank, true);
    };

    private ExtensionHooks() {
    }

    /** A station a profession has besides its Vanilla job site - see StationDefinition. */
    public static void registerStation(StationDefinition station) {
        Sections.register(station);
    }

    /** A group of the trade screen - see SectionDefinition. */
    public static void registerSection(SectionDefinition section) {
        Sections.register(section);
    }

    /**
     * The data component that marks an item as a mob's weapon (its value: the mob) - what an entry amount's
     * "mob" field asks for (data/ItemAmount, trade/RequiredEnchantmentCost). Without it, such an amount can never
     * be paid.
     */
    public static void setMobWeaponComponent(Supplier<? extends DataComponentType<EntityType<?>>> component) {
        mobWeaponComponent = component::get;
    }

    public static @Nullable DataComponentType<EntityType<?>> mobWeaponComponent() {
        return mobWeaponComponent.get();
    }

    /** Player XP an offer costs on top of its items (menu/VillagerMenu) - 0 unless an extension says otherwise. */
    public static void setPlayerXpCost(ToIntFunction<MerchantOffer> cost) {
        playerXpCost = cost;
    }

    public static int playerXpCost(@Nullable MerchantOffer offer) {
        return offer == null ? 0 : playerXpCost.applyAsInt(offer);
    }

    public static void setExplorerMaps(ExplorerMapBuilder builder) {
        explorerMaps = builder;
    }

    public static MapOutput explorerMap(Villager villager, Identifier entryId, ExplorerMap spec) {
        return explorerMaps.outputFor(villager, entryId, spec);
    }
}
