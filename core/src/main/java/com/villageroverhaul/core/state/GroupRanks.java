package com.villageroverhaul.core.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Rank invested in each of the four upgrade groups - see README.md. */
public record GroupRanks(int quest, int basicTrade, int masterTrade, int passive) {

    public static final GroupRanks EMPTY = new GroupRanks(0, 0, 0, 0);

    public static final Codec<GroupRanks> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("quest").forGetter(GroupRanks::quest),
            Codec.INT.fieldOf("basic_trade").forGetter(GroupRanks::basicTrade),
            Codec.INT.fieldOf("master_trade").forGetter(GroupRanks::masterTrade),
            Codec.INT.fieldOf("passive").forGetter(GroupRanks::passive)
    ).apply(instance, GroupRanks::new));

    public static final StreamCodec<ByteBuf, GroupRanks> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GroupRanks::quest,
            ByteBufCodecs.VAR_INT, GroupRanks::basicTrade,
            ByteBufCodecs.VAR_INT, GroupRanks::masterTrade,
            ByteBufCodecs.VAR_INT, GroupRanks::passive,
            GroupRanks::new
    );
}
