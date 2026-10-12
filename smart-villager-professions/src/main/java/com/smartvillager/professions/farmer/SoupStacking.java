package com.smartvillager.professions.farmer;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseRemainder;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Soups stack to 16 (global rule, 2026-09-30, Obsidian Farmer.md / Fisherman.md) - Vanilla's and other mods' alike. A soup is
 * recognised by what it is: an item that stacks to 1 and hands back a bowl when eaten (mushroom stew, rabbit stew, beetroot soup,
 * suspicious stew ...), so a soup of another mod needs no list. Runs last, so the other mods' default components are already in.
 * The bowl comes back per soup eaten, from a stack too (Vanilla's use remainder).
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class SoupStacking {

    public static final int STACK_SIZE = 16;

    private SoupStacking() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modifyMatching((item, components) -> isSoup(components), builder -> builder.set(DataComponents.MAX_STACK_SIZE, STACK_SIZE));
    }

    private static boolean isSoup(DataComponentGetter components) {
        Integer maxStackSize = components.get(DataComponents.MAX_STACK_SIZE);
        UseRemainder remainder = components.get(DataComponents.USE_REMAINDER);
        return maxStackSize != null && maxStackSize == 1 && remainder != null && remainder.convertInto().item().value() == Items.BOWL;
    }
}
