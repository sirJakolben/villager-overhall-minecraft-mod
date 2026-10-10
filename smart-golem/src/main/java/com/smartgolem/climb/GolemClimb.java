package com.smartgolem.climb;

import com.smartgolem.SmartGolemMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * A climb of an Iron Golem up a wall two blocks high, on the golem: when it started (game tick), where it stood (from), where the animation is drawn
 * (start) and where the golem really is - and ends - (target). Synced to the clients, which play the climb animation for it (client/); not saved - a
 * golem that is unloaded in the middle of a climb is just standing there afterwards.
 *
 * The golem is at the target from the first tick on and does not move during the climb: its model is drawn at the start instead (the client moves it
 * back by the difference, ClimbGolemClient), glides there from where it stood, and the animation ends exactly in the standing pose at the target - so
 * nothing jumps when it is over.
 */
public record GolemClimb(long startTick, Vec3 from, Vec3 start, Vec3 target) {

    public static final GolemClimb NONE = new GolemClimb(-1L, Vec3.ZERO, Vec3.ZERO, Vec3.ZERO);

    public static final StreamCodec<io.netty.buffer.ByteBuf, GolemClimb> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, GolemClimb::startTick,
            Vec3.STREAM_CODEC, GolemClimb::from,
            Vec3.STREAM_CODEC, GolemClimb::start,
            Vec3.STREAM_CODEC, GolemClimb::target,
            GolemClimb::new);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SmartGolemMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GolemClimb>> ATTACHMENT = ATTACHMENT_TYPES.register(
            "golem_climb",
            () -> AttachmentType.builder(() -> NONE).sync(STREAM_CODEC).build());

    public boolean active() {
        return startTick >= 0L;
    }
}
