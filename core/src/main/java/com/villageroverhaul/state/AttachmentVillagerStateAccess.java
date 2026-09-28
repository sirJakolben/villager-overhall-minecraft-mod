package com.villageroverhaul.state;

import com.villageroverhaul.station.ProfessionLock;
import com.villageroverhaul.trade.VillagerOffers;
import net.minecraft.world.entity.npc.villager.Villager;

final class AttachmentVillagerStateAccess implements VillagerStateAccess {

    private final Villager villager;

    AttachmentVillagerStateAccess(Villager villager) {
        this.villager = villager;
    }

    @Override
    public VillagerState getState() {
        return villager.getData(ModAttachments.VILLAGER_STATE);
    }

    /**
     * The single mutation point for villager state, so also the one place that keeps the villager's
     * Vanilla offer list (a derived view of this state) in sync - see VillagerOffers - and that locks
     * the profession as soon as the villager has any of our XP, from whatever source (2026-09-23).
     */
    @Override
    public void setState(VillagerState newState) {
        VillagerState oldState = getState();
        villager.setData(ModAttachments.VILLAGER_STATE, newState);
        if (!villager.level().isClientSide() && (newState.level() > 0 || newState.xp() > 0)) {
            ProfessionLock.lock(villager);
        }
        // The work scan writes XP/happiness every few seconds while a villager works; only rebuild the
        // offer list when something the offers are actually derived from has changed.
        if (affectsOffers(oldState, newState)) {
            VillagerOffers.refresh(villager);
        }
    }

    private static boolean affectsOffers(VillagerState oldState, VillagerState newState) {
        return oldState.level() != newState.level()
                || !oldState.ranks().equals(newState.ranks())
                || !oldState.stock().equals(newState.stock())
                || !oldState.stations().equals(newState.stations());
    }
}
