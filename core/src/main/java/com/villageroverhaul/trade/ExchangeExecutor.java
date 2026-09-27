package com.villageroverhaul.trade;

import com.villageroverhaul.data.ItemAmount;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Validates and performs a single exchange (one or two inputs for one output) against a player's inventory. */
public final class ExchangeExecutor {

    private ExchangeExecutor() {
    }

    public static boolean canAfford(Player player, ItemAmount input, Optional<ItemAmount> secondInput) {
        if (secondInput.isPresent() && secondInput.get().item() == input.item()) {
            return player.getInventory().countItem(input.item()) >= input.count() + secondInput.get().count();
        }
        return has(player, input) && secondInput.map(second -> has(player, second)).orElse(true);
    }

    public static void execute(Player player, ItemAmount input, Optional<ItemAmount> secondInput, ItemAmount output) {
        remove(player, input);
        secondInput.ifPresent(second -> remove(player, second));
        player.addItem(output.toStack(player.registryAccess()));
    }

    private static boolean has(Player player, ItemAmount amount) {
        return ContainerHelper.clearOrCountMatchingItems(player.getInventory(), amount::matches, Integer.MAX_VALUE, true) >= amount.count();
    }

    private static void remove(Player player, ItemAmount amount) {
        ContainerHelper.clearOrCountMatchingItems(player.getInventory(), amount::matches, amount.count(), false);
    }
}
