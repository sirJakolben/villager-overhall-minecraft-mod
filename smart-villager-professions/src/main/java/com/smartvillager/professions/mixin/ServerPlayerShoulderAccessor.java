package com.smartvillager.professions.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the boat's perch logic take a parrot off a player's shoulder (DecorPerch): Vanilla keeps the setters protected. */
@Mixin(ServerPlayer.class)
public interface ServerPlayerShoulderAccessor {

    @Invoker("setShoulderEntityLeft")
    void smartvillager_professions$setShoulderEntityLeft(CompoundTag tag);

    @Invoker("setShoulderEntityRight")
    void smartvillager_professions$setShoulderEntityRight(CompoundTag tag);
}
