package com.villageroverhaul.client.ui;

import com.villageroverhaul.api.SectionDefinition;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * How a section's rows look in the trade screen - the client half of api/SectionLogic, registered separately
 * (register, from a client-only class) so server code never loads screen classes. Every method defaults to the
 * standard trade row; the Trade Rework's quests move arrow and reward left and add a reroll button.
 */
public interface SectionClientLogic {

    /** Row-relative x of the arrow and the result icon in a standard row. */
    int ROW_ARROW_X = 50;
    int ROW_RESULT_X = 63;

    SectionClientLogic STANDARD = new SectionClientLogic() {
    };

    default int arrowX() {
        return ROW_ARROW_X;
    }

    default int resultX() {
        return ROW_RESULT_X;
    }

    /** An extra widget on a row (drawn above the row button, clipped to the list), or null. rowX/rowY: the row's corner. */
    default @Nullable RowWidget rowWidget(RowContext context, int slot, int rowX, int rowY) {
        return null;
    }

    /** What a row widget may use of the screen. */
    interface RowContext {
        Villager villager();

        SectionDefinition section();

        /** False for the part of a widget scrolled out of the list - a widget's isMouseOver should check it. */
        boolean isInList(double mouseX, double mouseY);

        /** Runs SectionLogic.onAction on the server (network/SectionActionPayload). */
        void sendAction(int slot, int action);
    }

    /** A row widget and the tooltip it shows while hovered (also while inactive). */
    record RowWidget(AbstractWidget widget, Supplier<Component> tooltip) {
    }

    Map<Identifier, SectionClientLogic> REGISTERED = new ConcurrentHashMap<>();

    static void register(Identifier section, SectionClientLogic logic) {
        REGISTERED.put(section, logic);
    }

    static SectionClientLogic of(Identifier section) {
        return REGISTERED.getOrDefault(section, STANDARD);
    }
}
