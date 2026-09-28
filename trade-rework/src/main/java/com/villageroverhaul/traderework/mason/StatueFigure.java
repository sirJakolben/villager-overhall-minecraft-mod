package com.villageroverhaul.traderework.mason;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.Arrays;
import java.util.Locale;

/**
 * Who a villager statue shows (2026-09-28): an adult with one of Vanilla's professions (or none, or the
 * nitwit), or a child. Rolled evenly by the server when the statue is placed and kept in the block state, so
 * the client knows it without any extra sync. Modded professions are not in the pool.
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
    BABY(VillagerProfession.NONE),
    /**
     * Not rolled yet: the state a statue is placed with, on the client too. Only the server rolls
     * (VillagerStatueBlock.onPlace), so the client never shows a guess of its own that the server then
     * replaces - until the server's figure arrives only the base plate shows. Never rolled itself.
     */
    PENDING(VillagerProfession.NONE);

    /** The pool: every figure but PENDING. */
    private static final StatueFigure[] ROLLABLE = Arrays.stream(values()).filter(figure -> figure != PENDING).toArray(StatueFigure[]::new);

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
        return ROLLABLE[random.nextInt(ROLLABLE.length)];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
