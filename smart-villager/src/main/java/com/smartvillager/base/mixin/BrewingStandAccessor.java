package com.smartvillager.base.mixin;

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The stand's blaze fuel charges (0-20, private in Vanilla) - set once by world/BrewingStandSpawns. */
@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandAccessor {

    @Accessor("fuel")
    void smartvillager$setFuel(int fuel);

    @Accessor("fuel")
    int smartvillager$getFuel();
}
