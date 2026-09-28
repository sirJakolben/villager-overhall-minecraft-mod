package com.villageroverhaul.traderework.client.ui;

import com.villageroverhaul.client.ui.VillagerGuiTextures;
import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;

/**
 * A quest row's reroll button, right of the reward (2026-09-26). Shows READY while the slot can be rerolled and
 * COOLDOWN during its cooldown; the server checks the cooldown again (QuestLogic.onAction), this only mirrors
 * it - re-read every frame, so it lights up once the cooldown ends.
 */
public class RerollButtonWidget extends AbstractButton {

    // Cropped from villager_gui/reroll_button_active/_inactive.png (canvas x 178, y 77).
    private static final VillagerGuiTextures.Sprite READY =
            VillagerGuiTextures.Sprite.of(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "reroll_button"), 15, 15);
    private static final VillagerGuiTextures.Sprite COOLDOWN =
            VillagerGuiTextures.Sprite.of(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "reroll_button_disabled"), 15, 15);

    private final BooleanSupplier ready;
    private final Runnable onClick;

    public RerollButtonWidget(int x, int y, BooleanSupplier ready, Runnable onClick) {
        super(x, y, READY.width(), READY.height(), Component.empty());
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
        VillagerGuiTextures.Sprite sprite = active ? READY : COOLDOWN;
        graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.texture(), getX(), getY(), 0.0F, 0.0F,
                getWidth(), getHeight(), sprite.width(), sprite.height());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
