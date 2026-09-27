package com.villageroverhaul.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * A quest row's reroll button, right of the reward (2026-09-26). Shows REROLL_BUTTON while the slot can
 * be rerolled and REROLL_BUTTON_DISABLED during its cooldown; the server checks the cooldown again
 * (QuestActions.reroll), this only mirrors it - re-read every frame, so it lights up once the cooldown ends.
 */
public class RerollButtonWidget extends AbstractButton {

    private final BooleanSupplier ready;
    private final Runnable onClick;

    public RerollButtonWidget(int x, int y, BooleanSupplier ready, Runnable onClick) {
        super(x, y, VillagerGuiTextures.REROLL_BUTTON.width(), VillagerGuiTextures.REROLL_BUTTON.height(), Component.empty());
        this.onClick = onClick;
        this.ready = ready;
        this.active = ready.getAsBoolean();
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onClick.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        active = ready.getAsBoolean();
        VillagerGuiTextures.Sprite sprite = active ? VillagerGuiTextures.REROLL_BUTTON : VillagerGuiTextures.REROLL_BUTTON_DISABLED;
        graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.texture(), getX(), getY(), 0.0F, 0.0F,
                getWidth(), getHeight(), sprite.width(), sprite.height());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
