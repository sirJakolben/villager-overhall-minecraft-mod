package com.villageroverhaul.traderework.trade;

import com.mojang.serialization.MapCodec;
import com.villageroverhaul.api.ExchangeExtensionType;
import com.villageroverhaul.api.ResultOverride;
import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.mason.MasonItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * An entry whose output is the villager statue of the trading villager's own type (2026-09-28): a desert
 * Mason sells desert statues, a taiga Mason taiga statues. Under "extensions": {"vo_trade_rework:villager_statue": {}},
 * replaces the entry's base_output (api/ResultOverride). Types without a statue (modded ones) get the plains statue.
 */
public enum VillagerStatueOutput implements ResultOverride {
    INSTANCE;

    public static final ExchangeExtensionType<VillagerStatueOutput> TYPE =
            new ExchangeExtensionType<>(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "villager_statue"), MapCodec.unit(INSTANCE).codec());

    @Override
    public Output resultFor(Villager villager, Identifier entryId) {
        DeferredItem<BlockItem> statue = villager.getVillagerData().type().unwrapKey()
                .map(MasonItems.VILLAGER_STATUES::get)
                .orElse(null);
        if (statue == null) {
            statue = MasonItems.VILLAGER_STATUES.get(VillagerType.PLAINS);
        }
        return new Output(new ItemStack(statue.get()), false);
    }
}
