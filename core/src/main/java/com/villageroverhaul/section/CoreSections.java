package com.villageroverhaul.section;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.api.ExtensionHooks;
import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.StationDefinition;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The core's own two sections, registered the same way an extension registers its own. Both work at the Vanilla
 * job site and restock normally, so both meters fill together there - only the Trades meter is shown. Every
 * profession built from its Vanilla trades (vanilla/VanillaCatalog) fills them: emerald rewards are Quests, the
 * rest Trades. A designed profession may use them too (the Trade Rework's basic trades are Trades).
 */
public final class CoreSections {

    public static final Identifier QUESTS = id("quests");
    public static final Identifier TRADES = id("trades");

    /** 9 points unlock everything (rank 6), 11 more max the prices out (Tweak-Werte.md). */
    public static final List<Integer> UPGRADE_COSTS = List.of(1, 1, 1, 2, 2, 2, 3, 3, 5);
    public static final int FULL_UNLOCK_RANK = 6;

    private static final Identifier TRADE_METER = id("basic_productivity");

    private CoreSections() {
    }

    public static void register() {
        ExtensionHooks.registerSection(SectionDefinition.builder(QUESTS)
                .order(0)
                .station(StationDefinition.JOB_SITE)
                .hiddenMeter()
                .upgradeCosts(UPGRADE_COSTS, FULL_UNLOCK_RANK)
                .build());
        ExtensionHooks.registerSection(SectionDefinition.builder(TRADES)
                .order(1)
                .station(StationDefinition.JOB_SITE)
                .meter(TRADE_METER)
                .upgradeCosts(UPGRADE_COSTS, FULL_UNLOCK_RANK)
                .build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, path);
    }
}
