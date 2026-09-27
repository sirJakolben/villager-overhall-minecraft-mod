package com.villageroverhaul.traderework.runesmith;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The Runesmith's upgrade template at the smithing table (Runesmith.md). The upgrade itself is a
 * Vanilla smithing_transform recipe; this only adds the one rule Vanilla doesn't have: durability carries
 * over relatively (mixin/SmithingUpgradeDurabilityMixin).
 */
public final class UpgradeTemplates {

    private UpgradeTemplates() {
    }

    public static boolean isOurs(ItemStack template) {
        return template.is(RunesmithItems.UPGRADE_TEMPLATE.get());
    }

    /**
     * Gives result the same share of wear the base had (rounded, never broken by it), and scales the Repair
     * Smith's padding the same way, capped at the new item's padding maximum.
     */
    public static void rescaleDurability(ItemStack base, ItemStack result) {
        if (!base.isDamageableItem() || !result.isDamageableItem()) {
            return;
        }
        double scale = result.getMaxDamage() / (double) base.getMaxDamage();
        result.setDamageValue(Mth.clamp((int) Math.round(base.getDamageValue() * scale), 0, result.getMaxDamage() - 1));
        int padding = BonusDurability.of(base);
        if (padding > 0) {
            BonusDurability.set(result, Math.min(BonusDurability.max(result), (int) Math.round(padding * scale)));
        }
    }
}
