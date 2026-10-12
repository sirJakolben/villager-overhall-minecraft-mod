package com.smartgolem;

import com.smartgolem.climb.GolemClimb;
import com.smartgolem.test.GolemGameTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Smart Iron Golem - a smarter Iron Golem, as native to Vanilla as possible (plan-iron-golem-ai.md): it patrols the village along its paths at night,
 * climbs walls two blocks high, follows an iron ingot and swims faster. The Vanilla AI stays; the mod adds to it through one narrow mixin on the
 * IronGolem (mixin/IronGolemMixin), subclasses of Vanilla's own navigation classes (ai/) and NeoForge events.
 */
@Mod(SmartGolemMod.MODID)
public class SmartGolemMod {

    public static final String MODID = "smartgolem";

    public SmartGolemMod(IEventBus modEventBus) {
        GolemClimb.ATTACHMENT_TYPES.register(modEventBus);
        GolemGameTests.register(modEventBus);
    }
}
