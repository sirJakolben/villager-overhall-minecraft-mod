package com.villageroverhaul.traderework.client.ui;

import com.villageroverhaul.traderework.station.ThreeInThreeOutMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Placeholder window for the 3 + 3 slot stations (crushing station, repair station) until they have their own texture: Vanilla's one-row chest
 * (generic_54), the three middle slots painted over with the background grey and the furnace arrow
 * drawn there - input left, output right.
 */
public class ThreeInThreeOutScreen extends AbstractContainerScreen<ThreeInThreeOutMenu> {

    private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier ARROW_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int ROW_HEIGHT = 18 + 17;
    private static final int BACKGROUND_GREY = 0xFFC6C6C6;

    public ThreeInThreeOutScreen(ThreeInThreeOutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 114 + 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = (this.width - this.imageWidth) / 2;
        int yo = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo, 0.0F, 0.0F, this.imageWidth, ROW_HEIGHT, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, xo, yo + ROW_HEIGHT, 0.0F, 126.0F, this.imageWidth, 96, 256, 256);
        graphics.fill(xo + 61, yo + 17, xo + 115, yo + 35, BACKGROUND_GREY);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW_SPRITE, xo + 76, yo + 18, 24, 16);
    }
}
