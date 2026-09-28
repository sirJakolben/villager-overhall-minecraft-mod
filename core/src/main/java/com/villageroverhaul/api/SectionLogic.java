package com.villageroverhaul.api;

import com.villageroverhaul.section.StandardSection;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.work.RestockService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;

/**
 * How a section behaves, on the server. Every method has the standard trade behavior as its default
 * (section/StandardSection), so a section only overrides what it does differently: the Trade Rework's quests
 * pick rotating slots instead of listing every entry, its passive works at its station instead of restocking.
 * The core only ever calls this interface - it knows no quest slots and no passives.
 *
 * Called for the sections that apply to the villager's profession (section/VillagerSections); the ones about
 * offers and meters only while the section is open.
 */
public interface SectionLogic {

    /** The standard behavior: every unlocked entry of the section, restocked by its meter. */
    SectionLogic STANDARD = new SectionLogic() {
    };

    /** The section's rows, in display order, scaled to its rank and with their stock. */
    default List<SectionOffer> offers(Villager villager, VillagerState state, SectionDefinition section) {
        return StandardSection.offers(villager, state, section);
    }

    /** After the player completed one of the section's offers (the items have already moved). */
    default void onUse(Villager villager, SectionDefinition section, SectionOffer offer) {
        StandardSection.recordUse(villager, section, offer);
    }

    /** How much stock the section misses right now (its largest gap) - drives the focus and the refill. */
    default int missingStock(Villager villager, VillagerState state, SectionDefinition section) {
        return StandardSection.missingStock(villager, state, section);
    }

    /** The meter is full and stock is missing: hand out stock and empty the meter. */
    default VillagerState refill(Villager villager, VillagerState state, SectionDefinition section) {
        return StandardSection.refill(villager, state, section);
    }

    /** Size of the section's meter in work points. A meter above its size (after a rank-up) counts as full. */
    default int meterPoints(Villager villager, VillagerState state, SectionDefinition section) {
        return RestockService.METER_POINTS;
    }

    /** Work is waiting at the section's station - the focus goes there before anything else. */
    default boolean isPending(ServerLevel level, Villager villager, VillagerState state, SectionDefinition section) {
        return false;
    }

    /** Every work scan (also outside work time, then workTime is false): the section's own work, if any. */
    default VillagerState onWorkScan(ServerLevel level, Villager villager, VillagerState state, SectionDefinition section, boolean workTime) {
        return state;
    }

    /**
     * Time-based changes of the section's own state - every tick while the trade screen is open, when it opens and
     * on every work scan. Offers are a pure view of the state, so this is what makes a paused row reappear.
     */
    default void refresh(Villager villager, SectionDefinition section) {
    }

    /** A row button other than the row itself (network/SectionActionPayload) - the menu is open, the player trades. */
    default void onAction(Villager villager, ServerPlayer player, SectionDefinition section, int slot, int action) {
    }

    /** Extra lines for /vo state sections. */
    default List<String> describe(Villager villager, VillagerState state, SectionDefinition section) {
        return List.of();
    }
}
