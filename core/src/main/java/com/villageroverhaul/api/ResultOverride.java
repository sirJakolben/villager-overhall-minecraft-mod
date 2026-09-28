package com.villageroverhaul.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

/**
 * An entry extension (ExchangeExtensionType) whose value decides the offer's result instead of base_output -
 * e.g. the Trade Rework's explorer maps, which point to a structure near this villager. Asked on the server
 * every time the offer list is rebuilt (trade/VillagerOffers), so keep it cheap or cache per villager.
 */
public interface ResultOverride {

    /** The offer's result; soldOut shows the offer sold out (e.g. nothing was found). */
    record Output(ItemStack stack, boolean soldOut) {
    }

    Output resultFor(Villager villager, Identifier entryId);
}
