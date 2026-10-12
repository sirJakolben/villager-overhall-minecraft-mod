package com.villageroverhaul.traderework.farmer;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Farmer's food (Obsidian Farmer.md, 2026-09-30): the golden crops the harvest basket makes (passive/HarvestWork) and the
 * dishes the glazing station makes of them (Masteries 1-4, exchange/farmer_*.json). Golden carrot and golden apple are Vanilla's.
 * The golden potato only waits for the Hot Potato (chain jump) - not a Mastery yet. Amounts, durations and the bread's effect are
 * placeholders (tweaks/farmer.md); the item textures too (they still point at Vanilla's textures, "Fehlende Texturen und Modelle.md").
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class FarmerItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeReworkMod.MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TradeReworkMod.MODID);

    /**
     * Candied apple's second half: nothing of its own, no icon, no particles - when it runs out, the drinker gets Speed
     * (SugarRush). That is how "longer Regeneration, then Speed" works with Vanilla's effects.
     */
    public static final DeferredHolder<MobEffect, SugarRushEffect> SUGAR_RUSH = MOB_EFFECTS.register("sugar_rush", SugarRushEffect::new);

    /** Food values of the golden crops: nutrition as the plain crop plus 3-4, saturation as Vanilla's golden carrot. */
    public static final DeferredItem<Item> GOLDEN_BREAD = ITEMS.registerSimpleItem("golden_bread",
            properties -> properties.food(new FoodProperties(8, 14.4F, false)));
    public static final DeferredItem<Item> GOLDEN_BEETROOT = ITEMS.registerSimpleItem("golden_beetroot",
            properties -> properties.food(new FoodProperties(4, 14.4F, false)));
    public static final DeferredItem<Item> GOLDEN_POTATO = ITEMS.registerSimpleItem("golden_potato",
            properties -> properties.food(new FoodProperties(6, 14.4F, false)));

    /** Master 1, golden apple: Regeneration 30 s, then Speed 60 s (SUGAR_RUSH runs as long as the Regeneration). */
    public static final DeferredItem<Item> CANDIED_APPLE = ITEMS.registerSimpleItem("candied_apple",
            properties -> properties.food(new FoodProperties(6, 14.4F, true), Consumables.defaultFood()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                            new MobEffectInstance(MobEffects.REGENERATION, 600, 1),
                            new MobEffectInstance(SUGAR_RUSH, 600, 0, false, false, false))))
                    .build()));
    /** Master 2, golden beetroot: Haste 2 min - a soup, so it stacks to 16 (and gives the bowl back). */
    public static final DeferredItem<Item> GOLDEN_BEETROOT_SOUP = ITEMS.registerSimpleItem("golden_beetroot_soup",
            properties -> properties.stacksTo(16).usingConvertsTo(Items.BOWL).food(new FoodProperties(10, 14.4F, true), Consumables.defaultFood()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.HASTE, 2400, 1)))
                    .build()));
    /** Master 3, golden carrot: Night Vision 3 min. */
    public static final DeferredItem<Item> GLAZED_CARROT = ITEMS.registerSimpleItem("glazed_carrot",
            properties -> properties.food(new FoodProperties(8, 14.4F, true), Consumables.defaultFood()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 0)))
                    .build()));
    /** Master 4, golden bread: Absorption 2 min (placeholder - the Obsidian note has no effect for the bread yet). */
    public static final DeferredItem<Item> GILDED_TOAST = ITEMS.registerSimpleItem("gilded_toast",
            properties -> properties.food(new FoodProperties(10, 14.4F, true), Consumables.defaultFood()
                    .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1)))
                    .build()));

    private FarmerItems() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            List.of(GOLDEN_BREAD, GOLDEN_BEETROOT, GOLDEN_POTATO, CANDIED_APPLE, GOLDEN_BEETROOT_SOUP, GLAZED_CARROT, GILDED_TOAST).forEach(event::accept);
        }
    }
}
