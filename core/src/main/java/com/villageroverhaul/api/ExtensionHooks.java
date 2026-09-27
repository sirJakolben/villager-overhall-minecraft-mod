package com.villageroverhaul.api;

import com.villageroverhaul.claim.ProfessionStations;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.data.ExplorerMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Where an extension mod (Trade Rework, split off 2026-09-28) plugs its professions into the core. Every hook
 * is neutral until something registers: no extra stations, no passives, no mob weapons, no extra XP price,
 * explorer-map offers sold out. That is exactly the core on its own - every profession then runs on the
 * non-implementation rule (fallback/FallbackCatalog).
 *
 * Register from the extension's mod constructor. The extension declares ordering AFTER the core, so the
 * core's registries exist by then; the maps here are concurrent because mod constructors may run in parallel.
 */
public final class ExtensionHooks {

    /**
     * A profession's passive (the work at its passive station): whether a step is due (the focus then goes to
     * the passive station), the step itself - called on every work scan, also outside work time (workTime
     * false) - and how many steps a full, 100 % happy work day brings at a passive rank (sizes the meter).
     */
    public interface PassiveHandler {
        boolean isPending(ServerLevel level, Villager villager, VillagerState state);

        VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime);

        int stepsPerDay(int passiveRank);
    }

    /** An explorer-map trade's result; soldOut when nothing was found. */
    public record MapOutput(ItemStack stack, boolean soldOut) {
    }

    @FunctionalInterface
    public interface ExplorerMapBuilder {
        MapOutput outputFor(Villager villager, Identifier tradeId, ExplorerMap spec);
    }

    private static final Map<ResourceKey<VillagerProfession>, PassiveHandler> PASSIVES = new ConcurrentHashMap<>();
    private static final Set<ResourceKey<VillagerProfession>> SECOND_HARD_SLOT = ConcurrentHashMap.newKeySet();

    private static volatile Supplier<@Nullable DataComponentType<EntityType<?>>> mobWeaponComponent = () -> null;
    private static volatile ToIntFunction<MerchantOffer> playerXpCost = offer -> 0;
    private static volatile ExplorerMapBuilder explorerMaps = (villager, tradeId, spec) -> {
        ItemStack blank = new ItemStack(Items.MAP);
        blank.set(DataComponents.ITEM_NAME, Component.translatable(spec.name()));
        return new MapOutput(blank, true);
    };

    private ExtensionHooks() {
    }

    /** A profession's master and passive stations - see claim/ProfessionStations. */
    public static void registerStations(ProfessionStations.Entry entry) {
        ProfessionStations.register(entry);
    }

    public static void registerPassive(ResourceKey<VillagerProfession> profession, PassiveHandler handler) {
        PASSIVES.put(profession, handler);
    }

    public static Optional<PassiveHandler> passive(Holder<VillagerProfession> profession) {
        return profession.unwrapKey().map(PASSIVES::get);
    }

    /** The profession gets a second hard quest slot instead of the permanent quest - see quest/QuestSlots. */
    public static void registerSecondHardSlot(ResourceKey<VillagerProfession> profession) {
        SECOND_HARD_SLOT.add(profession);
    }

    public static boolean hasSecondHardSlot(Holder<VillagerProfession> profession) {
        return profession.unwrapKey().map(SECOND_HARD_SLOT::contains).orElse(false);
    }

    /**
     * The data component that marks an item as a mob's weapon (its value: the mob) - what a trade or quest
     * amount's "mob" field asks for (data/ItemAmount, trade/RequiredEnchantmentCost). Without it, such an
     * amount can never be paid.
     */
    public static void setMobWeaponComponent(Supplier<? extends DataComponentType<EntityType<?>>> component) {
        mobWeaponComponent = component::get;
    }

    public static @Nullable DataComponentType<EntityType<?>> mobWeaponComponent() {
        return mobWeaponComponent.get();
    }

    /** Player XP a trade costs on top of its items (client/ui/VillagerMenu) - 0 unless an extension says otherwise. */
    public static void setPlayerXpCost(ToIntFunction<MerchantOffer> cost) {
        playerXpCost = cost;
    }

    public static int playerXpCost(@Nullable MerchantOffer offer) {
        return offer == null ? 0 : playerXpCost.applyAsInt(offer);
    }

    public static void setExplorerMaps(ExplorerMapBuilder builder) {
        explorerMaps = builder;
    }

    public static MapOutput explorerMap(Villager villager, Identifier tradeId, ExplorerMap spec) {
        return explorerMaps.outputFor(villager, tradeId, spec);
    }
}
