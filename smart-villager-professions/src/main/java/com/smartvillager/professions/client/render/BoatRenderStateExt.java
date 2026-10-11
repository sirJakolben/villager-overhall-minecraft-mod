package com.smartvillager.professions.client.render;

import com.smartvillager.professions.boat.BoatState;

/** What the boat renderer needs from the boat entity besides Vanilla's own render state - added to BoatRenderState by a mixin (mixin/BoatRenderStateMixin). */
public interface BoatRenderStateExt {

    BoatState smartvillager_professions$upgrades();

    void smartvillager_professions$setUpgrades(BoatState state);
}
