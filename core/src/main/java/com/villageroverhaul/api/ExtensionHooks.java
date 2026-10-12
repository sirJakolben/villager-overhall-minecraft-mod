package com.villageroverhaul.api;

import com.villageroverhaul.data.ExchangeExtensions;
import com.villageroverhaul.happiness.HappinessElements;
import com.villageroverhaul.section.Sections;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Where an extension mod (the Trade Rework) plugs into the core. The core registers its own two sections through
 * the same registerSection (section/CoreSections). Every other hook is neutral until something sets it: no mob
 * weapons, no extra XP price.
 *
 * Register from the extension's mod constructor. The extension declares ordering AFTER the core, so the core's
 * registries exist by then; registration is thread-safe because mod constructors may run in parallel. The two
 * setters hold one value each - the last extension to set one wins.
 */
public final class ExtensionHooks {

    private static volatile Supplier<@Nullable DataComponentType<EntityType<?>>> mobWeaponComponent = () -> null;
    private static volatile ToIntFunction<MerchantOffer> playerXpCost = offer -> 0;
    private static final java.util.List<Predicate<Villager>> STAY_PUT = new java.util.concurrent.CopyOnWriteArrayList<>();

    private ExtensionHooks() {
    }

    /**
     * A reason for a villager to stand still right now (the Fisherman while his cast is in the air): while it holds, Vanilla's place-bound behaviors
     * (strolling around the job site, walking back to it, meeting at the bell) pause exactly as for a station or a Trading Block call (station/PausedWhileAway).
     */
    public static void registerStayPut(Predicate<Villager> reason) {
        STAY_PUT.add(reason);
    }

    public static boolean isStayingPut(Villager villager) {
        for (Predicate<Villager> reason : STAY_PUT) {
            if (reason.test(villager)) {
                return true;
            }
        }
        return false;
    }

    /** A station a profession has besides its Vanilla job site - see StationDefinition. */
    public static void registerStation(StationDefinition station) {
        Sections.register(station);
    }

    /** A group of the trade screen - see SectionDefinition. */
    public static void registerSection(SectionDefinition section) {
        Sections.register(section);
    }

    /** Extra entry data under "extensions" - see ExchangeExtensionType. */
    public static void registerExchangeExtension(ExchangeExtensionType<?> type) {
        ExchangeExtensions.register(type);
    }

    /** An extra happiness element (e.g. a decoration nearby) - see HappinessElement. */
    public static void registerHappinessElement(HappinessElement element) {
        HappinessElements.register(element);
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
}
