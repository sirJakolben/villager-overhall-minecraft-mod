package com.smartvillager.professions.boat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * What a boat entity has been upgraded with (Obsidian Fisherman.md, 2026-10-12): a cannon, a mast, and the banner that is the mast's sail.
 * One synced attachment (both sides need it: the movement code, the seats, the sail and the jump bar are all computed client side too).
 * banner is the real banner item (pattern, colour, name) - empty while the mast is bare. lastShot: the game tick of the last cannon shot,
 * for the reload bar and the soot.
 */
public record BoatState(boolean cannon, boolean mast, ItemStack banner, long lastShot) {

    public static final long NEVER = -1_000_000L;
    public static final BoatState NONE = new BoatState(false, false, ItemStack.EMPTY, NEVER);

    public static final MapCodec<BoatState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("cannon", false).forGetter(BoatState::cannon),
            Codec.BOOL.optionalFieldOf("mast", false).forGetter(BoatState::mast),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("banner", ItemStack.EMPTY).forGetter(BoatState::banner),
            Codec.LONG.optionalFieldOf("last_shot", NEVER).forGetter(BoatState::lastShot)
    ).apply(instance, BoatState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoatState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BoatState::cannon,
            ByteBufCodecs.BOOL, BoatState::mast,
            ItemStack.OPTIONAL_STREAM_CODEC, BoatState::banner,
            ByteBufCodecs.VAR_LONG, BoatState::lastShot,
            BoatState::new
    );

    public BoatState withBanner(ItemStack newBanner) {
        return new BoatState(cannon, mast, newBanner, lastShot);
    }

    public BoatState withShot(long tick) {
        return new BoatState(cannon, mast, banner, tick);
    }

    public boolean hasBanner() {
        return mast && !banner.isEmpty();
    }

    /** Ticks until the cannon can be loaded again (0 = ready). */
    public int cooldown(long gameTime) {
        return (int) Math.max(0L, Cannon.COOLDOWN_TICKS - (gameTime - lastShot));
    }

    // ItemStack has no equals of its own - without these a state read back from disk would never equal the default.
    @Override
    public boolean equals(Object other) {
        return other instanceof BoatState state && cannon == state.cannon && mast == state.mast && lastShot == state.lastShot
                && ItemStack.matches(banner, state.banner);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Boolean.hashCode(cannon) + Boolean.hashCode(mast)) + Long.hashCode(lastShot);
    }
}
