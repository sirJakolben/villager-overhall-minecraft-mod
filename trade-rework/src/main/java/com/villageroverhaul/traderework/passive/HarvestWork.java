package com.villageroverhaul.traderework.passive;

import com.villageroverhaul.traderework.farmer.FarmerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The Farmer's passive (Obsidian Farmer.md, reworked 2026-10-07). He really works the field: every mature crop he harvests (farmer/HarvestCounting)
 * is one unit on his load, up to the capacity of his passive rank - 3 on rank 0, 9 on rank 6 - and the passive bar shows how full his load is.
 * A full load, or one that has not grown for a minute, sends him to the harvest basket (CarryingWork). There, every 5 seconds, one unit of
 * his load is taken out and rolled: goldChancePercent for the golden version of its crop (wheat: golden bread, beetroot, potato, carrot): 0 % on rank 0 up to 20 % on rank 6.
 * The winnings go straight into the basket. The crops themselves stay with him as in Vanilla.
 */
public final class HarvestWork extends CarryingWork {

    /** Chance in percent per unit for the golden version of its crop at the highest passive rank (Obsidian 02_Tweak-Werte); rank 0 has none, the steps between are even. */
    public static final double MAX_GOLD_CHANCE_PERCENT = 20.0;
    private static final int MAX_RANK = 6;

    /** The crops that count (by their block id) and the golden version of each. */
    private static final Map<Identifier, Supplier<? extends Item>> GOLD_BY_CROP = Map.of(
            Identifier.withDefaultNamespace("wheat"), FarmerItems.GOLDEN_BREAD,
            Identifier.withDefaultNamespace("beetroots"), FarmerItems.GOLDEN_BEETROOT,
            Identifier.withDefaultNamespace("potatoes"), FarmerItems.GOLDEN_POTATO,
            Identifier.withDefaultNamespace("carrots"), () -> Items.GOLDEN_CARROT);

    public static boolean counts(Identifier crop) {
        return GOLD_BY_CROP.containsKey(crop);
    }

    @Override
    protected SoundEvent workSound() {
        return SoundEvents.VILLAGER_WORK_FARMER;
    }

    /** 0 % on passive rank 0 up to MAX_GOLD_CHANCE_PERCENT on rank 6, evenly in between. */
    static double goldChancePercent(int passiveRank) {
        return MAX_GOLD_CHANCE_PERCENT * Math.clamp(passiveRank, 0, MAX_RANK) / MAX_RANK;
    }

    @Override
    protected List<ItemStack> process(ServerLevel level, BlockPos basket, Identifier crop, int passiveRank, RandomSource random) {
        List<ItemStack> won = new ArrayList<>();
        Supplier<? extends Item> gold = GOLD_BY_CROP.get(crop);
        if (gold != null && random.nextDouble() * 100.0 < goldChancePercent(passiveRank)) {
            won.add(new ItemStack(gold.get()));
        }
        return won;
    }
}
