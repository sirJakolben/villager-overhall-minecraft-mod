package com.smartgolem;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * The Iron Golem's attributes, the NeoForge way (EntityAttributeModificationEvent, no mixin):
 * - "tempt_range": every animal has it, the golem does not - Vanilla's TemptGoal reads it (the iron ingot lure, IronGolemMixin);
 * - the water movement efficiency (Vanilla: depth strider) pulls the swimming speed towards the walking speed: 0.1 of it makes the golem's terminal
 *   speed under water about 1.5 times what it was (acceleration and drag of Entity.travelInWater).
 */
@EventBusSubscriber(modid = SmartGolemMod.MODID)
public final class GolemAttributes {

    /** Vanilla's range for animals (blocks). */
    public static final double TEMPT_RANGE = 10.0;
    /** 0 = none, 1 = as fast as on land. */
    public static final double WATER_EFFICIENCY = 0.1;

    private GolemAttributes() {
    }

    @SubscribeEvent
    static void onAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.IRON_GOLEM, Attributes.TEMPT_RANGE, TEMPT_RANGE);
        event.add(EntityType.IRON_GOLEM, Attributes.WATER_MOVEMENT_EFFICIENCY, WATER_EFFICIENCY);
    }
}
