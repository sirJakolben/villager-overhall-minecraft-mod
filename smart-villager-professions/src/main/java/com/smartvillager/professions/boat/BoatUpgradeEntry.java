package com.smartvillager.professions.boat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.progression.api.ExchangeExtensionType;
import com.smartvillager.progression.api.ResultOverride;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * An entry that upgrades the player's own boat: under "extensions": {"smartvillager_professions:boat_upgrade": {"cannon": true}} (and/or
 * "mast": true). The entry's payment is the boat (second_input with a tag), its result here only shows the offer - a boat carrying the upgrade
 * - while the real result is made from the payment (BoatUpgrades.transform, api/ResultTransform).
 */
public record BoatUpgradeEntry(boolean cannon, boolean mast) implements ResultOverride {

    public static final ExchangeExtensionType<BoatUpgradeEntry> TYPE = new ExchangeExtensionType<>(
            Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, "boat_upgrade"),
            RecordCodecBuilder.<BoatUpgradeEntry>create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("cannon", false).forGetter(BoatUpgradeEntry::cannon),
                    Codec.BOOL.optionalFieldOf("mast", false).forGetter(BoatUpgradeEntry::mast)
            ).apply(instance, BoatUpgradeEntry::new)));

    @Override
    public Output resultFor(Villager villager, Identifier entryId) {
        ItemStack shown = new ItemStack(Items.OAK_BOAT);
        if (cannon) {
            shown.set(BoatUpgrades.CANNON.get(), Unit.INSTANCE);
        }
        if (mast) {
            shown.set(BoatUpgrades.MAST.get(), new BoatUpgrades.Mast(new ItemStack(Items.WHITE_BANNER)));
        }
        return new Output(shown, false);
    }
}
