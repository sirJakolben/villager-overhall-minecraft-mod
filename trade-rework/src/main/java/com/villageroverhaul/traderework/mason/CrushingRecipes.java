package com.villageroverhaul.traderework.mason;

import com.villageroverhaul.traderework.TradeReworkRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What the crushing station makes out of an item - always exactly one step back (Mason.md, 2026-09-26).
 * Two sources:
 * - Hand-made rules from the datapack (CrushingRule): cobblestone to gravel, sandstone to sand, glass to
 *   sand, concrete to powder, ... all 1:1. Rules "with variants" also redirect a root and all its
 *   variants: every stone kind goes back to its cobbled form (stone, smooth stone and stone bricks to
 *   cobblestone, deepslate to cobbled deepslate, smooth basalt to basalt).
 * - Every stonecutter variant back to its root block, derived from the stonecutter recipes themselves:
 *   a recipe "granite -> 1 polished granite stairs" becomes "1 polished granite stairs -> granite",
 *   "granite -> 2 slabs" becomes "2 slabs -> granite". The root is an input that isn't itself a
 *   stonecutter result (polished granite stairs go to granite, not polished granite). Using the
 *   stonecutter's own ratios means crushing can never make more than it took, and modded or future
 *   variants (the Salvager's gilded blocks) work with no extra data. Blocks that aren't stonecutter
 *   results - quartz block, blackstone, calcite - have no way back, so they stay final.
 *
 * The table is built once and rebuilt only when the recipes (/reload) or the rules change.
 */
public final class CrushingRecipes {

    /** inputCount of the input make 1 output. */
    public record Result(Item output, int inputCount) {
    }

    private record Edge(Item input, int count) {
    }

    private static RecipeManager builtFromRecipes;
    private static Registry<CrushingRule> builtFromRules;
    private static Map<Item, Result> table = Map.of();

    private CrushingRecipes() {
    }

    public static Optional<Result> of(ServerLevel level, Item item) {
        return Optional.ofNullable(table(level).get(item));
    }

    private static synchronized Map<Item, Result> table(ServerLevel level) {
        RecipeManager recipes = level.recipeAccess();
        Registry<CrushingRule> rules = level.registryAccess().lookupOrThrow(TradeReworkRegistries.CRUSHING);
        if (recipes != builtFromRecipes || rules != builtFromRules) {
            table = build(recipes, rules);
            builtFromRecipes = recipes;
            builtFromRules = rules;
        }
        return table;
    }

    private static Map<Item, Result> build(RecipeManager recipes, Registry<CrushingRule> rules) {
        Map<Item, List<Edge>> producers = new HashMap<>();
        for (RecipeHolder<StonecutterRecipe> holder : recipes.recipeMap().byType(RecipeType.STONECUTTING)) {
            StonecutterRecipe recipe = holder.value();
            recipe.input().items().map(Holder::value).forEach(input -> {
                ItemStack result = recipe.assemble(new SingleRecipeInput(new ItemStack(input)));
                if (!result.isEmpty() && result.getItem() != input) {
                    producers.computeIfAbsent(result.getItem(), item -> new ArrayList<>()).add(new Edge(input, result.getCount()));
                }
            });
        }

        Map<Item, Item> rootRedirects = new HashMap<>();
        for (CrushingRule rule : rules) {
            for (CrushingRule.Pair pair : rule.pairs()) {
                if (pair.withVariants()) {
                    rootRedirects.put(pair.input(), pair.output());
                }
            }
        }

        Map<Item, Result> built = new HashMap<>();
        for (Item variant : producers.keySet()) {
            root(variant, producers, new HashSet<>()).ifPresent(result -> built.put(variant,
                    new Result(rootRedirects.getOrDefault(result.output(), result.output()), result.inputCount())));
        }
        for (CrushingRule rule : rules) {
            for (CrushingRule.Pair pair : rule.pairs()) {
                built.put(pair.input(), new Result(pair.output(), 1));
            }
        }
        return Map.copyOf(built);
    }

    /** The root block a variant comes from, and how many of the variant one root block makes. */
    private static Optional<Result> root(Item variant, Map<Item, List<Edge>> producers, Set<Item> visiting) {
        List<Edge> edges = producers.get(variant);
        if (edges == null || !visiting.add(variant)) {
            return Optional.empty();
        }
        for (Edge edge : edges) {
            if (!producers.containsKey(edge.input())) {
                return Optional.of(new Result(edge.input(), edge.count()));
            }
        }
        for (Edge edge : edges) {
            Optional<Result> deeper = root(edge.input(), producers, visiting);
            if (deeper.isPresent()) {
                return Optional.of(new Result(deeper.get().output(), edge.count() * deeper.get().inputCount()));
            }
        }
        return Optional.empty();
    }
}
