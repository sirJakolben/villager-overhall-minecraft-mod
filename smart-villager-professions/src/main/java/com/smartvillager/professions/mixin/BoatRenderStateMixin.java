package com.smartvillager.professions.mixin;

import com.smartvillager.professions.boat.BoatState;
import com.smartvillager.professions.client.render.BoatRenderStateExt;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** The boat entity's upgrades (cannon, mast, banner) travel to the renderer in its render state. */
@Mixin(BoatRenderState.class)
public abstract class BoatRenderStateMixin implements BoatRenderStateExt {

    @Unique
    private BoatState smartvillager_professions$upgrades = BoatState.NONE;

    @Override
    public BoatState smartvillager_professions$upgrades() {
        return smartvillager_professions$upgrades;
    }

    @Override
    public void smartvillager_professions$setUpgrades(BoatState state) {
        this.smartvillager_professions$upgrades = state;
    }

    @Unique
    private double[] smartvillager_professions$offsets = new double[0];

    @Override
    public double[] smartvillager_professions$offsets() {
        return smartvillager_professions$offsets;
    }

    @Override
    public void smartvillager_professions$setOffsets(double[] offsets) {
        this.smartvillager_professions$offsets = offsets;
    }
}
