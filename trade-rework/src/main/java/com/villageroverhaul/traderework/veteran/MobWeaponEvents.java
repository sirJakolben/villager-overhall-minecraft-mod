package com.villageroverhaul.traderework.veteran;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Optional;

/**
 * Mob weapons in play (Veteran.md, 2026-09-26), all through NeoForge events - no mixin:
 * - A listed mob joining the world gets its main-hand item marked (plus uncommon rarity: yellow name like
 *   Vanilla's special items, 2026-09-26) and that slot's drop chance raised to
 *   MobWeapons.DROP_CHANCE. The drop itself stays Vanilla (Mob.dropCustomDeathLoot: only for player kills,
 *   Looting counts, the item comes out damaged). Items a mob picked up are "preserved" drops and stay
 *   unmarked, so a player's own sword never turns into a mob weapon.
 * - A player hitting something in melee with a mob weapon passes the mob's effect on (EFFECT_CHANCE,
 *   EFFECT_DURATION_FACTOR of the mob's duration). Arrows shot from a mob-weapon bow or crossbow carry it
 *   instead, rolled once when shot - like the Stray's own arrows.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class MobWeaponEvents {

    private static final Identifier ATTACK_SPEED_BONUS_ID = Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "mob_weapon_attack_speed");

    private MobWeaponEvents() {
    }

    @SubscribeEvent
    static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getEntity() instanceof Mob mob) {
            markWeapon(level, mob);
        } else if (event.getEntity() instanceof Arrow arrow && !event.loadedFromDisk() && arrow.getOwner() instanceof Player player) {
            ItemStack weapon = arrow.getWeaponItem();
            if (weapon != null) {
                effectFor(level, weapon, player).ifPresent(arrow::addEffect);
            }
        }
    }

    private static void markWeapon(ServerLevel level, Mob mob) {
        ItemStack weapon = mob.getItemBySlot(EquipmentSlot.MAINHAND);
        if (weapon.isEmpty() || MobWeapons.mobOf(weapon).isPresent() || mob.getDropChances().isPreserved(EquipmentSlot.MAINHAND)
                || MobWeapons.definition(level, mob.getType()).isEmpty()) {
            return;
        }
        weapon.set(MobWeapons.MOB_WEAPON.get(), mob.getType());
        weapon.set(DataComponents.RARITY, Rarity.UNCOMMON);
        mob.setDropChance(EquipmentSlot.MAINHAND, MobWeapons.DROP_CHANCE);
    }

    @SubscribeEvent
    static void onLivingDamage(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(event.getEntity().level() instanceof ServerLevel level) || !source.isDirect() || !(source.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack weapon = source.getWeaponItem();
        if (weapon != null) {
            LivingEntity target = event.getEntity();
            effectFor(level, weapon, player).ifPresent(effect -> target.addEffect(effect, player));
        }
    }


    /**
     * A shorter attack cooldown for mob weapons whose definition has one (e.g. the vindicator's axe, 20 %):
     * attack speed x 1 / (1 - reduction), added whenever the game asks for the item's modifiers - on both
     * sides, so the tooltip shows it. Computed from the mark, not stored on the item, so a smithing upgrade
     * (iron -> diamond axe) keeps the bonus on top of the new item's own speed.
     */
    @SubscribeEvent
    static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        MobWeapons.mobOf(event.getItemStack())
                .flatMap(MobWeapons::definition)
                .filter(definition -> definition.cooldownReduction() > 0.0F && definition.cooldownReduction() < 1.0F)
                .ifPresent(definition -> event.addModifier(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ATTACK_SPEED_BONUS_ID, 1.0 / (1.0 - definition.cooldownReduction()) - 1.0,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        EquipmentSlotGroup.MAINHAND));
    }

    /** The effect this weapon passes on this time - empty if it has none or the roll fails. */
    private static Optional<MobEffectInstance> effectFor(ServerLevel level, ItemStack weapon, Player user) {
        return MobWeapons.mobOf(weapon)
                .flatMap(mob -> MobWeapons.definition(level, mob))
                .flatMap(MobWeaponDefinition::effect)
                .filter(effect -> user.getRandom().nextFloat() < MobWeapons.EFFECT_CHANCE)
                .map(effect -> new MobEffectInstance(effect.effect(),
                        Math.max(1, Math.round(effect.duration() * MobWeapons.EFFECT_DURATION_FACTOR)), effect.amplifier()));
    }
}
