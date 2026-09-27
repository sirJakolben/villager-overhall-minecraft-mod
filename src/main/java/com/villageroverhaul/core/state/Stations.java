package com.villageroverhaul.core.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

/**
 * The villager's three workplaces (Block W, README.md), each a
 * reserved point of interest - see StationClaims. Any one of them makes it a Librarian (all three are
 * Vanilla "librarian job sites"); Vanilla's JOB_SITE memory always points at one of them and is
 * mirrored into its slot here, because the brain isn't synced to the client and the trade screen needs
 * to know which groups are open (ProgressionService.isGroupOpen).
 *
 * heldByStation: the villager's profession is being kept by a backup station - with two or more
 * stations and no experience yet, Vanilla would otherwise fire it the moment its job-site block breaks.
 *
 * tradingBlock: the Trading Block the player assigned it to with an emerald (Block B) - while that block
 * is powered, the villager walks there and stays (TradingBlockCall). Not a workplace, not counted.
 */
public record Stations(Optional<GlobalPos> basic, Optional<GlobalPos> master, Optional<GlobalPos> passive, boolean heldByStation,
                       Optional<GlobalPos> tradingBlock) {

    public static final Stations NONE = new Stations(Optional.empty(), Optional.empty(), Optional.empty(), false, Optional.empty());

    public static final Codec<Stations> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("basic_pos").forGetter(Stations::basic),
            GlobalPos.CODEC.optionalFieldOf("master_pos").forGetter(Stations::master),
            GlobalPos.CODEC.optionalFieldOf("passive_pos").forGetter(Stations::passive),
            Codec.BOOL.optionalFieldOf("held_by_station", false).forGetter(Stations::heldByStation),
            GlobalPos.CODEC.optionalFieldOf("trading_block_pos").forGetter(Stations::tradingBlock)
    ).apply(instance, Stations::new));

    public static final StreamCodec<ByteBuf, Stations> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), Stations::basic,
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), Stations::master,
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), Stations::passive,
            ByteBufCodecs.BOOL, Stations::heldByStation,
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC), Stations::tradingBlock,
            Stations::new
    );

    public Optional<GlobalPos> get(StationSlot slot) {
        return switch (slot) {
            case BASIC -> basic;
            case MASTER -> master;
            case PASSIVE -> passive;
        };
    }

    public Stations with(StationSlot slot, Optional<GlobalPos> pos) {
        return switch (slot) {
            case BASIC -> new Stations(pos, master, passive, heldByStation, tradingBlock);
            case MASTER -> new Stations(basic, pos, passive, heldByStation, tradingBlock);
            case PASSIVE -> new Stations(basic, master, pos, heldByStation, tradingBlock);
        };
    }

    public Stations withHeldByStation(boolean held) {
        return new Stations(basic, master, passive, held, tradingBlock);
    }

    public Stations withTradingBlock(Optional<GlobalPos> pos) {
        return new Stations(basic, master, passive, heldByStation, pos);
    }

    public int count() {
        return (basic.isPresent() ? 1 : 0) + (master.isPresent() ? 1 : 0) + (passive.isPresent() ? 1 : 0);
    }
}
