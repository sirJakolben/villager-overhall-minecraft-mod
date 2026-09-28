package com.villageroverhaul.api;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One group of the trade screen (register with ExtensionHooks.registerSection): its own rank with an upgrade
 * button, its own work meter, the entries (trades or quests) whose "section" names it, optionally a station and
 * optionally its own logic. The core registers Quests and Trades (section/CoreSections); the Trade Rework adds
 * its own. Build one with builder(id).
 *
 * - order: position in the screen, lowest first; meters are drawn in the same order, right to left.
 * - display: LIST is a group of rows in the scrolling list; BADGE is the badge at the top left (a section
 *   without rows, like a passive). Only one badge fits the screen: a villager with several BADGE sections shows
 *   the first by order, the others keep their meter but no badge - so give each profession at most one.
 * - station: where the villager works on this section - its meter fills there, and the section opens once the
 *   villager owns that station (see section/VillagerSections). Empty: always open, meter never fills.
 * - meterVisible / meterSprite: whether the meter shows in the screen, and its art - the textures
 *   "<namespace>:textures/gui/villager/<path>_background.png" and "..._current.png".
 * - upgradeCosts: the price of each rank-up (its length is the max rank); up to fullUnlockRank the ranks unlock
 *   entries, the ranks after it scale entries from their base to their max values (trade/ExchangeScaling).
 */
public record SectionDefinition(Identifier id, String titleKey, int order, Display display, Optional<Identifier> station,
                                boolean meterVisible, @Nullable Identifier meterSprite, List<Integer> upgradeCosts, int fullUnlockRank,
                                SectionLogic logic) {

    public enum Display {
        LIST, BADGE
    }

    public int maxRank() {
        return upgradeCosts.size();
    }

    /** Points the upgrade from currentRank to currentRank + 1 costs, or -1 once the section is maxed. */
    public int upgradeCost(int currentRank) {
        return currentRank >= 0 && currentRank < upgradeCosts.size() ? upgradeCosts.get(currentRank) : -1;
    }

    public static Builder builder(Identifier id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final Identifier id;
        private String titleKey;
        private int order;
        private Display display = Display.LIST;
        private Optional<Identifier> station = Optional.empty();
        private boolean meterVisible = true;
        private Identifier meterSprite;
        private List<Integer> upgradeCosts = List.of();
        private int fullUnlockRank;
        private SectionLogic logic = SectionLogic.STANDARD;

        private Builder(Identifier id) {
            this.id = id;
            this.titleKey = "section." + id.getNamespace() + "." + id.getPath();
        }

        /** Default: "section.<namespace>.<path>". */
        public Builder title(String translationKey) {
            this.titleKey = translationKey;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder badge() {
            this.display = Display.BADGE;
            return this;
        }

        public Builder station(Identifier station) {
            this.station = Optional.of(station);
            return this;
        }

        public Builder meter(Identifier sprite) {
            this.meterSprite = sprite;
            return this;
        }

        public Builder hiddenMeter() {
            this.meterVisible = false;
            return this;
        }

        public Builder upgradeCosts(List<Integer> costs, int fullUnlockRank) {
            this.upgradeCosts = List.copyOf(costs);
            this.fullUnlockRank = fullUnlockRank;
            return this;
        }

        public Builder logic(SectionLogic logic) {
            this.logic = logic;
            return this;
        }

        public SectionDefinition build() {
            if (meterVisible) {
                Objects.requireNonNull(meterSprite, "a visible meter needs a sprite: " + id);
            }
            return new SectionDefinition(id, titleKey, order, display, station, meterVisible, meterSprite, upgradeCosts, fullUnlockRank, logic);
        }
    }
}
