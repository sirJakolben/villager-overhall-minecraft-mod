package com.villageroverhaul.traderework.client.render;

import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.librarian.LibrarianEntities;
import com.villageroverhaul.traderework.veteran.VeteranBlockEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = TradeReworkMod.MODID)
public final class ModEntityRenderers {

    private ModEntityRenderers() {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // The thrown bottle is drawn as its item, exactly like Vanilla's thrown Bottle o' Enchanting.
        event.registerEntityRenderer(LibrarianEntities.VILLAGER_EXPERIENCE_BOTTLE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(LibrarianEntities.VILLAGER_EXPERIENCE_ORB.get(), VillagerExperienceOrbRenderer::new);
        event.registerBlockEntityRenderer(VeteranBlockEntities.WEAPON_RACK.get(), WeaponRackRenderer::new);
    }
}
