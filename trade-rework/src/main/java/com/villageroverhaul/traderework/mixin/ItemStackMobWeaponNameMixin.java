package com.villageroverhaul.traderework.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.villageroverhaul.traderework.veteran.MobWeapons;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A mob weapon's name, built when it is shown (Veteran.md, 2026-09-26): the mob's name plus the item's
 * own name, "Zombie Iron Sword" / "Zombie-Eisenschwert" (lang key item.villageroverhaul.mob_weapon). Built
 * from the current item, so a smithing upgrade renames it on its own ("Zombie Diamond Sword"). Only the
 * item name changes - an anvil rename (custom name) still wins, as in Vanilla. Every other stack is
 * untouched.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMobWeaponNameMixin {

    @ModifyReturnValue(method = "getItemName", at = @At("RETURN"))
    private Component villageroverhaul$mobWeaponName(Component original) {
        ItemStack self = (ItemStack) (Object) this;
        return MobWeapons.mobOf(self)
                .<Component>map(mob -> Component.translatable("item.vo_trade_rework.mob_weapon", mob.getDescription(), original))
                .orElse(original);
    }
}
