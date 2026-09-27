package com.villageroverhaul.veteran;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CrossbowItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Tooltip line for mob weapons that pass their mob's effect on (2026-09-26): "+ Applies Wither effect",
 * blue like Vanilla's positive attribute lines. Read from the synced mob-weapon list (MobWeapons.definition),
 * so it stays right after a smithing upgrade and follows datapack changes.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID, value = Dist.CLIENT)
public final class MobWeaponTooltip {

    private MobWeaponTooltip() {
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        MobWeapons.mobOf(event.getItemStack())
                .flatMap(MobWeapons::definition)
                .flatMap(MobWeaponDefinition::effect)
                .ifPresent(effect -> event.getToolTip().add(Component.translatable("item.villageroverhaul.mob_weapon.effect",
                        effect.effect().value().getDisplayName()).withStyle(ChatFormatting.BLUE)));
        // Faster draw (piglin crossbow, 2026-09-27) - only on crossbows, where it does something.
        if (event.getItemStack().getItem() instanceof CrossbowItem) {
            MobWeapons.mobOf(event.getItemStack())
                    .flatMap(MobWeapons::definition)
                    .map(MobWeaponDefinition::chargeReduction)
                    .filter(reduction -> reduction > 0.0F)
                    .ifPresent(reduction -> event.getToolTip().add(Component.translatable("item.villageroverhaul.mob_weapon.charge",
                            Math.round(reduction * 100)).withStyle(ChatFormatting.BLUE)));
        }
    }
}
