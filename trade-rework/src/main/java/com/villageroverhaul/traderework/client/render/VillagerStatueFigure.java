package com.villageroverhaul.traderework.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.villageroverhaul.traderework.mason.StatueFigure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.BabyVillagerModel;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.metadata.animation.VillagerMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Draws one grey statue - armor stand base plate plus villager - for the block (VillagerStatueRenderer) and the
 * item (VillagerStatueSpecialRenderer). The villager is built exactly like Vanilla's VillagerRenderer and
 * VillagerProfessionLayer draw a real one: base skin, then the biome clothes (with or without their hat,
 * depending on the profession's hat), then the profession clothes - only each texture is its grey copy
 * (GrayscaleTextures). No level badge and no animation: a statue with arms crossed, head straight.
 *
 * Expects the pose stack in entity model space (y down, feet at y = 24 px), as LivingEntityRenderer sets it up.
 */
public final class VillagerStatueFigure {

    private static final Identifier ARMOR_STAND = Identifier.withDefaultNamespace("textures/entity/armorstand/armorstand.png");
    private static final Identifier ADULT_SKIN = Identifier.withDefaultNamespace("textures/entity/villager/villager.png");
    private static final Identifier BABY_SKIN = Identifier.withDefaultNamespace("textures/entity/villager/villager_baby.png");
    /** Height of the base plate - the villager stands on it. */
    private static final float PLATE_HEIGHT = 1.0F / 16.0F;
    /** A villager standing still: no head turn, no walking, not unhappy. */
    private static final VillagerRenderState POSE = new VillagerRenderState();

    private final VillagerModel adult;
    private final VillagerModel adultNoHat;
    private final VillagerModel baby;
    private final VillagerModel babyNoHat;
    private final ModelPart basePlate;
    private final Map<Identifier, VillagerMetadataSection.Hat> hats = new HashMap<>();

    public VillagerStatueFigure(EntityModelSet models) {
        adult = new VillagerModel(models.bakeLayer(ModelLayers.VILLAGER));
        adultNoHat = new VillagerModel(models.bakeLayer(ModelLayers.VILLAGER_NO_HAT));
        baby = new BabyVillagerModel(models.bakeLayer(ModelLayers.VILLAGER_BABY));
        babyNoHat = new BabyVillagerModel(models.bakeLayer(ModelLayers.VILLAGER_BABY_NO_HAT));
        basePlate = models.bakeLayer(ModelLayers.ARMOR_STAND).getChild("base_plate");
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, ResourceKey<VillagerType> type, StatueFigure figure,
                       int lightCoords, int overlayCoords, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        collector.submitModelPart(basePlate, poseStack, RenderTypes.entityCutout(GrayscaleTextures.of(ARMOR_STAND)), lightCoords,
                overlayCoords, null, -1, crumbling);

        poseStack.pushPose();
        poseStack.translate(0.0F, -PLATE_HEIGHT, 0.0F);
        boolean isBaby = figure.isBaby();
        VillagerModel model = isBaby ? baby : adult;
        collector.submitModel(model, POSE, poseStack, model.renderType(GrayscaleTextures.of(isBaby ? BABY_SKIN : ADULT_SKIN)),
                lightCoords, overlayCoords, -1, null, outlineColor, crumbling);

        // Same hat rule as VillagerProfessionLayer: the biome hat hides under a full profession hat.
        Identifier typeTexture = villagerTexture(isBaby ? "baby" : "type", type.identifier());
        Identifier professionTexture = villagerTexture("profession", figure.profession().identifier());
        VillagerMetadataSection.Hat typeHat = hat(villagerTexture("type", type.identifier()));
        VillagerMetadataSection.Hat professionHat = hat(professionTexture);
        boolean typeHatVisible = professionHat == VillagerMetadataSection.Hat.NONE
                || professionHat == VillagerMetadataSection.Hat.PARTIAL && typeHat != VillagerMetadataSection.Hat.FULL;
        VillagerModel typeModel = typeHatVisible ? model : isBaby ? babyNoHat : adultNoHat;
        collector.order(1).submitModel(typeModel, POSE, poseStack, RenderTypes.entityCutout(GrayscaleTextures.of(typeTexture)),
                lightCoords, overlayCoords, -1, null, outlineColor, null);
        if (!isBaby && figure.profession() != VillagerProfession.NONE) {
            collector.order(2).submitModel(model, POSE, poseStack, RenderTypes.entityCutout(GrayscaleTextures.of(professionTexture)),
                    lightCoords, overlayCoords, -1, null, outlineColor, null);
        }
        poseStack.popPose();
    }

    /** Outline of an adult on its plate, for fitting the item into the GUI slot. */
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        basePlate.getExtentsForGui(poseStack, output);
        poseStack.translate(0.0F, -PLATE_HEIGHT, 0.0F);
        adult.setupAnim(POSE);
        adult.root().getExtentsForGui(poseStack, output);
    }

    private static Identifier villagerTexture(String folder, Identifier key) {
        return key.withPath(path -> "textures/entity/villager/" + folder + "/" + path + ".png");
    }

    /** The texture's "villager" metadata (hat none / partial / full), read once per texture. */
    private VillagerMetadataSection.Hat hat(Identifier texture) {
        return hats.computeIfAbsent(texture, id -> Minecraft.getInstance().getResourceManager().getResource(id).flatMap(resource -> {
            try {
                return resource.metadata().getSection(VillagerMetadataSection.TYPE).map(VillagerMetadataSection::hat);
            } catch (IOException e) {
                return Optional.empty();
            }
        }).orElse(VillagerMetadataSection.Hat.NONE));
    }
}
