package com.villageroverhaul.traderework.runesmith;

import com.mojang.serialization.Codec;
import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Runesmith's padding (Runesmith.md, 2026-09-26): on its highest passive rank the repair station
 * keeps working on a fully repaired item and adds up to PERCENT of its max durability on top. The padding
 * is a component (BONUS) - the item's own durability stays Vanilla. Damage takes the padding first
 * (mixin/ItemStackBonusDurabilityMixin), after Unbreaking; once it is used up the item wears as usual. Only
 * the Runesmith fills it: the anvil, Mending and grindstone only ever touch the Vanilla durability.
 * Shown as a tooltip line.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID, value = Dist.CLIENT)
public final class BonusDurability {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TradeReworkMod.MODID);

    /** Padding points left on this item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BONUS =
            DATA_COMPONENTS.registerComponentType("bonus_durability",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Padding on top of the Vanilla max durability, in percent (Tweak-Werte.md). */
    public static final int PERCENT = 10;
    /** Passive rank from which the repair station adds padding - the highest (Tweak-Werte.md). */
    public static final int FROM_PASSIVE_RANK = 6;

    private BonusDurability() {
    }

    public static int of(ItemStack stack) {
        return stack.getOrDefault(BONUS.get(), 0);
    }

    /** How much padding this item can hold: PERCENT of its max durability, rounded up. */
    public static int max(ItemStack stack) {
        return stack.isDamageableItem() ? Mth.ceil(stack.getMaxDamage() * PERCENT / 100.0) : 0;
    }

    public static void set(ItemStack stack, int points) {
        if (points > 0) {
            stack.set(BONUS.get(), points);
        } else {
            stack.remove(BONUS.get());
        }
    }

    /** Takes damage out of the padding first; returns what is left for the Vanilla durability. */
    public static int absorb(ItemStack stack, int damage) {
        int padding = of(stack);
        if (damage <= 0 || padding <= 0) {
            return damage;
        }
        int absorbed = Math.min(padding, damage);
        set(stack, padding - absorbed);
        return damage - absorbed;
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        int padding = of(event.getItemStack());
        if (padding > 0) {
            event.getToolTip().add(Component.translatable("item.vo_trade_rework.bonus_durability", padding).withStyle(ChatFormatting.AQUA));
        }
    }
}
