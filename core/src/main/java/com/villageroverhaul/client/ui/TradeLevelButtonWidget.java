package com.villageroverhaul.client.ui;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * The one small button graphic (trade_level_button.png) reused in three roles, per the 2026-09-22
 * UI decision: a group header's rank display + invest-a-point button, a trade row's current-stock
 * display (not clickable), and a quest row's reroll button. onClick null means "display only".
 * Height can differ from the source sprite's own 13px (the texture is then stretched) - rows use
 * this to make the button fill the full row band. drawTexture=false (2026-09-23: rows only, for
 * now) skips the background sprite entirely and shows just the label text.
 */
public class TradeLevelButtonWidget extends AbstractButton {

    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int ARROW_GAP = 1;
    private static final double ARROW_BOB_PERIOD_MS = 1000.0;

    private final String label;
    private final boolean drawTexture;
    @Nullable
    private final Runnable onClick;
    private boolean showArrow;

    public TradeLevelButtonWidget(int x, int y, String label, @Nullable Runnable onClick) {
        this(x, y, VillagerGuiTextures.TRADE_LEVEL_BUTTON.height(), true, label, onClick);
    }

    public TradeLevelButtonWidget(int x, int y, int height, boolean drawTexture, String label, @Nullable Runnable onClick) {
        super(x, y, VillagerGuiTextures.TRADE_LEVEL_BUTTON.width(), height, Component.literal(label));
        this.label = label;
        this.drawTexture = drawTexture;
        this.onClick = onClick;
        this.active = onClick != null;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (onClick != null) {
            onClick.run();
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (drawTexture) {
            VillagerGuiTextures.Sprite sprite = VillagerGuiTextures.TRADE_LEVEL_BUTTON;
            graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.texture(), getX(), getY(), 0.0F, 0.0F, getWidth(), getHeight(), sprite.width(), sprite.height());
        }

        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(label);
        graphics.text(font, label, getX() + (getWidth() - textWidth) / 2, getY() + (getHeight() - 8) / 2, LABEL_COLOR);

        if (showArrow) {
            // Up arrow left of the button, bobbing gently (one pixel up and down).
            VillagerGuiTextures.Sprite arrow = VillagerGuiTextures.TRADE_LEVEL_ARROW;
            int bob = (int) Math.round(Math.sin(Util.getMillis() / ARROW_BOB_PERIOD_MS * 2.0 * Math.PI));
            int arrowX = getX() - arrow.width() - ARROW_GAP;
            int arrowY = getY() + (getHeight() - arrow.height()) / 2 + bob;
            graphics.blit(RenderPipelines.GUI_TEXTURED, arrow.texture(), arrowX, arrowY, 0.0F, 0.0F,
                    arrow.width(), arrow.height(), arrow.width(), arrow.height());
        }
    }

    /** Shown while the group's next upgrade is affordable with the villager's unspent points. */
    public void setShowArrow(boolean showArrow) {
        this.showArrow = showArrow;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
