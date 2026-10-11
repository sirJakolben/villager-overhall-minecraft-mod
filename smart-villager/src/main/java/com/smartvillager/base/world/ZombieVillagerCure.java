package com.smartvillager.base.world;

import com.smartvillager.base.SmartVillagerMod;
import com.smartvillager.base.mixin.ZombieVillagerInvoker;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Any golden food cures a zombie villager - no weakness potion needed. Vanilla wants a golden apple and the weakness effect; here the item
 * tag smartvillager:golden_food (golden apples, golden carrot; other mods add their golden foods to it) is enough. The cure itself is
 * Vanilla's: the same 3 to 5 minutes, the same strength effect and shaking, the same conversion. Without golden food in hand the click stays
 * Vanilla's. Done in NeoForge's interaction event, no mixin on the interaction.
 */
@EventBusSubscriber(modid = SmartVillagerMod.MODID)
public final class ZombieVillagerCure {

    public static final TagKey<Item> GOLDEN_FOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SmartVillagerMod.MODID, "golden_food"));
    /** Vanilla's cure time: 3600 + random(2401) ticks. */
    private static final int MIN_TICKS = 3600;
    private static final int EXTRA_TICKS = 2401;

    private ZombieVillagerCure() {
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ZombieVillager zombieVillager)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(GOLDEN_FOOD)) {
            return;
        }
        // Both sides, so the client doesn't run Vanilla's own interaction on top.
        event.setCanceled(true);
        if (zombieVillager.isConverting()) {
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!zombieVillager.level().isClientSide()) {
            stack.consume(1, event.getEntity());
            ((ZombieVillagerInvoker) zombieVillager).smartvillager$startConverting(event.getEntity().getUUID(),
                    MIN_TICKS + zombieVillager.getRandom().nextInt(EXTRA_TICKS));
        }
    }
}
