package com.smartvillager.base.mixin;

import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;

/** Vanilla's private "start the cure" - the weakness cure (world/ZombieVillagerCure) starts it as soon as the effect is on. */
@Mixin(ZombieVillager.class)
public interface ZombieVillagerInvoker {

    @Invoker("startConverting")
    void smartvillager$startConverting(@Nullable UUID player, int time);
}
