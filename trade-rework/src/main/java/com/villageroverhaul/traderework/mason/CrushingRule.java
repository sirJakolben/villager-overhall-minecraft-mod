package com.villageroverhaul.traderework.mason;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * One datapack file of the crushing station's hand-made rules ("Zerkleinern" in Mason.md), in
 * data/villageroverhaul/villageroverhaul/crushing/: each pair turns 1 input into 1 output (all 1:1,
 * decided 2026-09-26). Everything that is a stonecutter variant needs no rule - CrushingRecipes derives
 * those from the stonecutter recipes. A rule here wins over a derived one for the same input.
 *
 * with_variants (default false): the rule also redirects every stonecutter variant whose root is the
 * input, at the stonecutter's ratio - "stone -> cobblestone" then turns stone bricks, stone slabs and
 * chiseled stone bricks into cobblestone too, instead of stone (2026-09-26: every stone kind goes back to
 * its cobbled form).
 */
public record CrushingRule(List<Pair> pairs) {

    public record Pair(Item input, Item output, boolean withVariants) {
        public static final Codec<Pair> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("input").forGetter(Pair::input),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("output").forGetter(Pair::output),
                Codec.BOOL.optionalFieldOf("with_variants", false).forGetter(Pair::withVariants)
        ).apply(instance, Pair::new));
    }

    public static final Codec<CrushingRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Pair.CODEC.listOf().fieldOf("pairs").forGetter(CrushingRule::pairs)
    ).apply(instance, CrushingRule::new));
}
