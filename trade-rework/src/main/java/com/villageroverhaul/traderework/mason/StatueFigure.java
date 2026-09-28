package com.villageroverhaul.traderework.mason;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.Locale;

/**
 * Who a villager statue shows (2026-09-28): an adult with one of Vanilla's professions (or none, or the
 * nitwit), or a child. Rolled evenly when the statue is placed and kept in the block state, so the client
 * knows it without any extra sync. Modded professions are not in the pool.
 */
public enum StatueFigure implements StringRepresentable {
    NONE(VillagerProfession.NONE),
    ARMORER(VillagerProfession.ARMORER),
    BUTCHER(VillagerProfession.BUTCHER),
    CARTOGRAPHER(VillagerProfession.CARTOGRAPHER),
    CLERIC(VillagerProfession.CLERIC),
    FARMER(VillagerProfession.FARMER),
    FISHERMAN(VillagerProfession.FISHERMAN),
    FLETCHER(VillagerProfession.FLETCHER),
    LEATHERWORKER(VillagerProfession.LEATHERWORKER),
    LIBRARIAN(VillagerProfession.LIBRARIAN),
    MASON(VillagerProfession.MASON),
    NITWIT(VillagerProfession.NITWIT),
    SHEPHERD(VillagerProfession.SHEPHERD),
    TOOLSMITH(VillagerProfession.TOOLSMITH),
    WEAPONSMITH(VillagerProfession.WEAPONSMITH),
    /** A child - no profession clothes, like Vanilla's baby villagers. */
    BABY(VillagerProfession.NONE);

    private static final StatueFigure[] VALUES = values();

    private final ResourceKey<VillagerProfession> profession;

    StatueFigure(ResourceKey<VillagerProfession> profession) {
        this.profession = profession;
    }

    public ResourceKey<VillagerProfession> profession() {
        return profession;
    }

    public boolean isBaby() {
        return this == BABY;
    }

    public static StatueFigure random(RandomSource random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
