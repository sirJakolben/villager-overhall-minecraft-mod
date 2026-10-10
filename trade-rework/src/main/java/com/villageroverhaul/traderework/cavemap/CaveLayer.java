package com.villageroverhaul.traderework.cavemap;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.StringRepresentable;

/** One of the two height bands a cave map is bound to (CaveMapKind holds the limits); the name shows in the map's tooltip (lang key cave_layer.*). */
public enum CaveLayer implements StringRepresentable {

    BOTTOM("bottom"),
    TOP("top");

    public static final Codec<CaveLayer> CODEC = StringRepresentable.fromEnum(CaveLayer::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, CaveLayer> STREAM_CODEC =
            ByteBufCodecs.idMapper(index -> values()[index], CaveLayer::ordinal).cast();

    private final String id;

    CaveLayer(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
