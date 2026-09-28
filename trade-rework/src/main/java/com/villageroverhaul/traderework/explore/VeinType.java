package com.villageroverhaul.traderework.explore;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Vein kinds with Vanilla OreVeinifier's height band and toggle sign (copper: positive, y 0..50; iron: negative, y -60..-8). */
public enum VeinType implements StringRepresentable {
    COPPER("copper", 0, 50, true),
    IRON("iron", -60, -8, false);

    public static final Codec<VeinType> CODEC = StringRepresentable.fromEnum(VeinType::values);

    private final String serializedName;
    private final int minY;
    private final int maxY;
    private final boolean positiveToggle;

    VeinType(String serializedName, int minY, int maxY, boolean positiveToggle) {
        this.serializedName = serializedName;
        this.minY = minY;
        this.maxY = maxY;
        this.positiveToggle = positiveToggle;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public boolean positiveToggle() {
        return positiveToggle;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
