package com.smartvillager.professions.client.render;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.fisherman.FishermanEntities;
import com.smartvillager.professions.librarian.LibrarianEntities;
import com.smartvillager.professions.mason.MasonBlockEntities;
import com.smartvillager.professions.veteran.VeteranBlockEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = SmartVillagerProfessionsMod.MODID)
public final class ModEntityRenderers {

    private ModEntityRenderers() {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // The thrown bottle is drawn as its item, exactly like Vanilla's thrown Bottle o' Enchanting.
        event.registerEntityRenderer(LibrarianEntities.VILLAGER_EXPERIENCE_BOTTLE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(LibrarianEntities.VILLAGER_EXPERIENCE_ORB.get(), VillagerExperienceOrbRenderer::new);
        event.registerEntityRenderer(FishermanEntities.VILLAGER_BOBBER.get(), VillagerBobberRenderer::new);
        event.registerBlockEntityRenderer(VeteranBlockEntities.WEAPON_RACK.get(), WeaponRackRenderer::new);
        event.registerBlockEntityRenderer(MasonBlockEntities.VILLAGER_STATUE.get(), VillagerStatueRenderer::new);
    }

    /** Item model type "smartvillager_professions:villager_statue" (items/*_villager_statue.json). */
    @SubscribeEvent
    static void onRegisterSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, "villager_statue"), VillagerStatueSpecialRenderer.Unbaked.MAP_CODEC);
    }
}
