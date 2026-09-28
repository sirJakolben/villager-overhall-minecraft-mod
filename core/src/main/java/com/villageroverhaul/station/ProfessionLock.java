package com.villageroverhaul.station;

import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Keeps a villager's profession the Vanilla way (README.md):
 * Vanilla's ResetProfession only fires villagers whose own trade XP is 0 at merchant level 1. Our
 * offers carry no Vanilla XP (so Vanilla never levels them), which would leave that value at 0 forever -
 * so any villager that has been traded with or has any of our villager XP gets it set to 1, and
 * Vanilla's own rule then keeps the profession. 1 stays far below the 10 XP of a Vanilla level-up.
 */
public final class ProfessionLock {

    private ProfessionLock() {
    }

    public static void lock(Villager villager) {
        if (villager.getVillagerXp() == 0) {
            villager.setVillagerXp(1);
        }
    }
}
