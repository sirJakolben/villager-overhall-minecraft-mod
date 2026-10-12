package com.smartvillager.professions.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * The boat chest (Obsidian Fisherman.md, 2026-10-12): the chest of Vanilla's chest boat, 12 x 8 x 12 with a 4 px lid - 2 px smaller than a chest block - on the
 * layout of the normal chest texture until it has its own texture: the body and the lid are Vanilla's own 14 x 10 x 14 and 14 x 5 x 14 boxes with Vanilla's UVs,
 * scaled down to 12 x 8 x 12 and 12 x 4 x 12, so every face shows its whole piece of the texture (a smaller box on the same UV layout would cut the pieces off);
 * the lock is Vanilla's 2 x 4 x 1. Shared by the chest in the boat (BoatUpgradeRenderer) and
 * the item (BoatChestSpecialRenderer). Block space like Vanilla's ChestModel: centred in its block, Y up, the lock at +Z.
 */
final class BoatChestModel {

    /** The sprite of the normal chest, in Vanilla's chest atlas. */
    static final SpriteId SPRITE = Sheets.CHEST_REGULAR.select(ChestType.SINGLE);

    private BoatChestModel() {
    }

    static ModelPart bake() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(-7.0F, 0.0F, -7.0F, 14.0F, 10.0F, 14.0F), PartPose.offset(8.0F, 0.0F, 8.0F));
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F), PartPose.offset(8.0F, 8.0F, 2.0F));
        root.addOrReplaceChild("lock", CubeListBuilder.create().texOffs(0, 0).addBox(7.0F, -3.0F, 12.0F, 2.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 8.0F, 2.0F));
        ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        scale(baked.getChild("bottom"), 12.0F / 14.0F, 8.0F / 10.0F, 12.0F / 14.0F);
        scale(baked.getChild("lid"), 12.0F / 14.0F, 4.0F / 5.0F, 12.0F / 14.0F);
        return baked;
    }

    private static void scale(ModelPart part, float x, float y, float z) {
        part.xScale = x;
        part.yScale = y;
        part.zScale = z;
    }
}
