package com.villageroverhaul.veteran;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.data.ModDataPackRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * Mob weapons (Veteran.md, 2026-09-26): the normal Vanilla item, marked with the mob that carried it
 * (MOB_WEAPON). The mark is all that differs - the item works exactly like its Vanilla self everywhere
 * (damage, enchanting, anvil, smithing upgrades keep it). It shows in the name (mixin/ItemStackMobWeaponNameMixin:
 * "Zombie Iron Sword", built from the current item, so it stays right after an upgrade), Veteran
 * quests ask for it (ItemAmount.mob), and it passes the mob's hit effect on (MobWeaponEvents).
 */
public final class MobWeapons {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, VillagerOverhaulMod.MODID);

    /** The mob that carried this item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EntityType<?>>> MOB_WEAPON =
            DATA_COMPONENTS.registerComponentType("mob_weapon",
                    builder -> builder.persistent(BuiltInRegistries.ENTITY_TYPE.byNameCodec()).networkSynchronized(ByteBufCodecs.registry(Registries.ENTITY_TYPE)));

    /** Chance a listed mob drops its (marked) weapon when a player kills it - Vanilla 0.085 (Tweak-Werte.md). */
    public static final float DROP_CHANCE = 0.25F;
    /** Chance per hit that a mob weapon passes its mob's effect on (Tweak-Werte.md). */
    public static final float EFFECT_CHANCE = 0.2F;
    /** Share of the mob's own effect duration the weapon gives (Tweak-Werte.md). */
    public static final float EFFECT_DURATION_FACTOR = 0.5F;

    private MobWeapons() {
    }

    public static Optional<EntityType<?>> mobOf(ItemStack stack) {
        return Optional.ofNullable(stack.get(MOB_WEAPON.get()));
    }

    /** The datapack entry for this mob, if it is a mob-weapon mob. */
    public static Optional<MobWeaponDefinition> definition(Level level, EntityType<?> mob) {
        for (MobWeaponDefinition definition : level.registryAccess().lookupOrThrow(ModDataPackRegistries.MOB_WEAPON)) {
            if (definition.entity() == mob) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    /**
     * Same without a level, on either side (NeoForge's CommonHooks.resolveLookup: the server's registries, or
     * the client level's synced copy) - for places that only have the item, like its attribute modifiers.
     */
    public static Optional<MobWeaponDefinition> definition(EntityType<?> mob) {
        HolderLookup.RegistryLookup<MobWeaponDefinition> lookup = CommonHooks.resolveLookup(ModDataPackRegistries.MOB_WEAPON);
        if (lookup == null) {
            return Optional.empty();
        }
        return lookup.listElements().map(Holder::value).filter(definition -> definition.entity() == mob).findFirst();
    }
}
