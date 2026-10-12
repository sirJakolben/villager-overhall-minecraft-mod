package com.smartvillager.professions.boat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What is built into a boat entity (Obsidian Fisherman.md, 2026-10-12), one synced attachment - both sides need it: seats, speed, the jump bar and the
 * drawing are computed client side too. slots: per slot of the boat, front to back, the id of the part in it (BoatParts.EMPTY = free; the list may be
 * shorter than the boat's slots). deco: the one decoration (the mast) with the slot it belongs to and its item (for the mast: with its banner).
 * lastShot: game tick of the cannon's last shot (reload bar, soot). parrotLockUntil: until this game tick no parrot perches (after one was thrown out).
 * The chest contents are not here - they are server-only (BoatInventory).
 */
public record BoatState(List<Identifier> slots, Optional<Deco> deco, long lastShot, long parrotLockUntil) {

    public static final long NEVER = -1_000_000L;
    public static final BoatState NONE = new BoatState(List.of(), Optional.empty(), NEVER, NEVER);

    public record Deco(Identifier type, int slot, ItemStack stack) {
        public static final Codec<Deco> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("type").forGetter(Deco::type),
                Codec.INT.fieldOf("slot").forGetter(Deco::slot),
                ItemStack.OPTIONAL_CODEC.fieldOf("stack").forGetter(Deco::stack)
        ).apply(instance, Deco::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Deco> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Deco::type,
                ByteBufCodecs.VAR_INT, Deco::slot,
                ItemStack.OPTIONAL_STREAM_CODEC, Deco::stack,
                Deco::new
        );

        @Override
        public boolean equals(Object other) {
            return other instanceof Deco deco && type.equals(deco.type) && slot == deco.slot && ItemStack.matches(stack, deco.stack);
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + slot;
        }
    }

    public static final MapCodec<BoatState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(BoatState::slots),
            Deco.CODEC.optionalFieldOf("deco").forGetter(BoatState::deco),
            Codec.LONG.optionalFieldOf("last_shot", NEVER).forGetter(BoatState::lastShot),
            Codec.LONG.optionalFieldOf("parrot_lock_until", NEVER).forGetter(BoatState::parrotLockUntil)
    ).apply(instance, BoatState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoatState> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), BoatState::slots,
            Deco.STREAM_CODEC.apply(ByteBufCodecs::optional), BoatState::deco,
            ByteBufCodecs.VAR_LONG, BoatState::lastShot,
            ByteBufCodecs.VAR_LONG, BoatState::parrotLockUntil,
            BoatState::new
    );

    /** The part in this slot, or BoatParts.EMPTY. */
    public Identifier part(int slot) {
        return slot >= 0 && slot < slots.size() ? slots.get(slot) : BoatParts.EMPTY;
    }

    public boolean isFree(int slot) {
        return part(slot).equals(BoatParts.EMPTY);
    }

    /** How many slots are filled - each is a seat less. */
    public int filledSlots() {
        return (int) slots.stream().filter(id -> !id.equals(BoatParts.EMPTY)).count();
    }

    public int count(BoatPartType type) {
        return (int) slots.stream().filter(type.id()::equals).count();
    }

    /** The cannon sits in the frontmost slot only. */
    public boolean cannon() {
        return part(0).equals(BoatParts.CANNON.id());
    }

    public int chests() {
        return count(BoatParts.CHEST);
    }

    public boolean mast() {
        return deco.filter(d -> d.type().equals(BoatParts.MAST.id())).isPresent();
    }

    /** The slot the mast belongs to, or -1. */
    public int mastSlot() {
        return mast() ? deco.get().slot() : -1;
    }

    /** The mast's sail: the banner on its item (empty while the mast is bare). */
    public ItemStack banner() {
        return deco.filter(d -> d.type().equals(BoatParts.MAST.id()))
                .map(d -> {
                    BoatUpgrades.Mast mast = d.stack().get(BoatUpgrades.MAST.get());
                    return mast == null ? ItemStack.EMPTY : mast.banner();
                })
                .orElse(ItemStack.EMPTY);
    }

    public boolean hasBanner() {
        return !banner().isEmpty();
    }

    public BoatState withPart(int slot, Identifier part) {
        List<Identifier> list = new ArrayList<>(slots);
        while (list.size() <= slot) {
            list.add(BoatParts.EMPTY);
        }
        list.set(slot, part);
        return new BoatState(List.copyOf(list), deco, lastShot, parrotLockUntil);
    }

    public BoatState withDeco(Optional<Deco> newDeco) {
        return new BoatState(slots, newDeco, lastShot, parrotLockUntil);
    }

    public BoatState withBanner(ItemStack banner) {
        if (!mast()) {
            return this;
        }
        Deco old = deco.get();
        ItemStack stack = old.stack().copy();
        stack.set(BoatUpgrades.MAST.get(), new BoatUpgrades.Mast(banner));
        return withDeco(Optional.of(new Deco(old.type(), old.slot(), stack)));
    }

    public BoatState withShot(long tick) {
        return new BoatState(slots, deco, tick, parrotLockUntil);
    }

    public BoatState withParrotLock(long untilTick) {
        return new BoatState(slots, deco, lastShot, untilTick);
    }

    /** Ticks until the cannon can be loaded again (0 = ready). */
    public int cooldown(long gameTime) {
        return (int) Math.max(0L, Cannon.COOLDOWN_TICKS - (gameTime - lastShot));
    }

    // ItemStack has no equals of its own - without these a state read back from disk would never equal the default.
    @Override
    public boolean equals(Object other) {
        return other instanceof BoatState state && slots.equals(state.slots) && deco.equals(state.deco)
                && lastShot == state.lastShot && parrotLockUntil == state.parrotLockUntil;
    }

    @Override
    public int hashCode() {
        return 31 * slots.hashCode() + Long.hashCode(lastShot);
    }
}
