package com.villageroverhaul.traderework.cage;

/** Implemented by the Iron Golem through IronGolemMixin: the cached "is there an empty cage around" that decides whether it spares zombie villagers. */
public interface CageAware {

    boolean vo$emptyCageNear();
}
