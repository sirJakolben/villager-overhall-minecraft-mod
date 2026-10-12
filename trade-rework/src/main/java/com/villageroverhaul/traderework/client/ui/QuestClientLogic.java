package com.villageroverhaul.traderework.client.ui;

import com.villageroverhaul.client.ui.SectionClientLogic;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.traderework.quest.QuestLogic;
import com.villageroverhaul.traderework.quest.QuestSlots;
import com.villageroverhaul.traderework.quest.QuestState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Quest rows in the trade screen (2026-09-26): a quest never has a second payment, so arrow and reward move
 * left and a reroll button takes the room on the right - on every slot except the permanent quest, which never
 * rotates. The arrow still clears the discounted count (up to +31).
 */
public final class QuestClientLogic implements SectionClientLogic {

    private static final int ARROW_X = 34;
    private static final int RESULT_X = 46;
    private static final int REROLL_X = 65;
    private static final int REROLL_Y = 2;
    private static final long TICKS_PER_DAY = 24000L;

    @Override
    public int arrowX() {
        return ARROW_X;
    }

    @Override
    public int resultX() {
        return RESULT_X;
    }

    @Override
    public RowWidget rowWidget(RowContext context, int slot, int rowX, int rowY) {
        if (slot == QuestSlots.PERMANENT_SLOT) {
            return null;
        }
        Villager villager = context.villager();
        RerollButtonWidget button = new RerollButtonWidget(rowX + REROLL_X, rowY + REROLL_Y,
                () -> QuestLogic.rerollCooldownTicks(QuestState.of(villager), villager, slot) <= 0,
                () -> context.sendAction(slot, QuestLogic.REROLL)) {
            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                return super.isMouseOver(mouseX, mouseY) && context.isInList(mouseX, mouseY);
            }
        };
        return new RowWidget(button, () -> rerollTooltip(context, slot));
    }

    /**
     * Ready: "Reroll quest". During the cooldown a day timer (2026-09-26): one diamond per day of the full
     * cooldown, a filled one per day still to wait - a 3-day cooldown shows ◆◆◆, then ◆◆◇, on the last
     * day ◆◇◇. The full cooldown is taken at the current quest rank (like the one the reroll set), never shorter
     * than the days actually left.
     */
    private static Component rerollTooltip(RowContext context, int slot) {
        Villager villager = context.villager();
        long remaining = QuestLogic.rerollCooldownTicks(QuestState.of(villager), villager, slot);
        if (remaining <= 0) {
            return Component.translatable("gui.vo_trade_rework.quest_reroll");
        }
        int daysLeft = (int) ((remaining + TICKS_PER_DAY - 1) / TICKS_PER_DAY);
        int rank = VillagerStateAccess.of(villager).getState().rank(context.section().id());
        int totalDays = Math.max(daysLeft, QuestLogic.rerollCooldownDays(rank, context.section()));
        return Component.literal("◆".repeat(daysLeft) + "◇".repeat(totalDays - daysLeft));
    }
}
