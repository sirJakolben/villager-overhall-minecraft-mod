package com.villageroverhaul.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * A section's rank: shows it, and a click spends points on the next rank (network/InvestUpgradePointPayload).
 * While that rank is affordable, an arrow bobs left of the button (setShowArrow).
 */
public class RankButtonWidget extends AbstractButton {

    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int ARROW_GAP = 1;
    private static final double ARROW_BOB_PERIOD_MS = 1000.0;

    private final String label;
    private final Runnable onClick;
    private boolean showArrow;

    public RankButtonWidget(int x, int y, int rank, Runnable onClick) {
        super(x, y, VillagerGuiTextures.RANK_BUTTON.width(), VillagerGuiTextures.RANK_BUTTON.height(), Component.literal(String.valueOf(rank)));
        this.label = String.valueOf(rank);
        this.onClick = onClick;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onClick.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        VillagerGuiTextures.Sprite sprite = VillagerGuiTextures.RANK_BUTTON;
        graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.texture(), getX(), getY(), 0.0F, 0.0F, getWidth(), getHeight(), sprite.width(), sprite.height());

        Font font = Minecraft.getInstance().font;
        graphics.text(font, label, getX() + (getWidth() - font.width(label)) / 2, getY() + (getHeight() - 8) / 2, LABEL_COLOR);

        if (showArrow) {
            // Up arrow left of the button, bobbing gently (one pixel up and down).
            VillagerGuiTextures.Sprite arrow = VillagerGuiTextures.RANK_ARROW;
            int bob = (int) Math.round(Math.sin(Util.getMillis() / ARROW_BOB_PERIOD_MS * 2.0 * Math.PI));
            int arrowX = getX() - arrow.width() - ARROW_GAP;
            int arrowY = getY() + (getHeight() - arrow.height()) / 2 + bob;
            graphics.blit(RenderPipelines.GUI_TEXTURED, arrow.texture(), arrowX, arrowY, 0.0F, 0.0F,
                    arrow.width(), arrow.height(), arrow.width(), arrow.height());
        }
    }

    /** Shown while the section's next rank is affordable with the villager's unspent points. */
    public void setShowArrow(boolean showArrow) {
        this.showArrow = showArrow;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
