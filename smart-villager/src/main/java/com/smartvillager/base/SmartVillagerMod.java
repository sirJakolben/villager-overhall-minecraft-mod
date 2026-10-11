package com.smartvillager.base;

import com.smartvillager.base.test.VillagerGameTests;
import com.smartvillager.base.trading.TradingBlockLinks;
import com.smartvillager.base.trading.TradingBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Smart Villager - the bottom rung of the ladder (Vanilla, Smart Villager, Progression, Professions; see docs/smart-villager.md).
 * Small additions to Vanilla villagers that need nothing else: the emerald (lure, claim a workplace), the Trading Block, more zombie
 * villagers that any golden food cures, fuelled brewing stands. Progression and Professions plug into it through api/SmartVillagerApi.
 */
@Mod(SmartVillagerMod.MODID)
public class SmartVillagerMod {

    public static final String MODID = "smartvillager";

    public SmartVillagerMod(IEventBus modEventBus) {
        TradingBlocks.BLOCKS.register(modEventBus);
        TradingBlocks.ITEMS.register(modEventBus);
        TradingBlockLinks.ATTACHMENT_TYPES.register(modEventBus);
        VillagerGameTests.register(modEventBus);
    }
}
