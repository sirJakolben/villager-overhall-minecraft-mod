package com.smartvillager.professions.boat;

/** The boat's own seat count, before our parts took any - implemented on AbstractBoat by mixin/AbstractBoatMixin (getMaxPassengers is protected). */
public interface BoatSeats {

    int smartvillager_professions$rawSeats();
}
