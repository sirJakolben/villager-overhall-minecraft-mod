package com.villageroverhaul.core.state;

import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.progression.UpgradeGroup;

/**
 * A villager's three workplaces. Declared in morning order (decided 2026-09-24): at the start of work
 * the villager walks to its master station first, else its basic one (lectern), else its passive one -
 * the same order breaks ties in the focus choice (RestockService). The focus index stored in
 * DailyProductivity is the ordinal.
 */
public enum StationSlot {
    MASTER(UpgradeGroup.MASTER_TRADE),
    BASIC(UpgradeGroup.BASIC_TRADE),
    PASSIVE(UpgradeGroup.PASSIVE);

    private final UpgradeGroup group;

    StationSlot(UpgradeGroup group) {
        this.group = group;
    }

    public UpgradeGroup group() {
        return group;
    }

    /** The trade tier restocked at this station - none for the passive station. */
    public ItemExchange.Tier tier() {
        return switch (this) {
            case MASTER -> ItemExchange.Tier.MASTER;
            case BASIC -> ItemExchange.Tier.BASIC;
            case PASSIVE -> null;
        };
    }

    public static StationSlot of(ItemExchange.Tier tier) {
        return tier == ItemExchange.Tier.MASTER ? MASTER : BASIC;
    }
}
