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
 * shorter than the boat's slots). decos: the decorations - at most one of each kind (sail, stern) - each with the slot it belongs to and its item (for the sail: with its banner).
 * lastShot: game tick of the cannon's last shot (reload bar, soot). petLockUntil: until this game tick no parrot, cat or dog boards (after they were thrown out).
 * The chest contents are not here - they are server-only (BoatInventory).
 */
public record BoatState(List<Identifier> slots, List<Deco> decos, long lastShot, long petLockUntil) {

    public static final long NEVER = -1_000_000L;
    public static final BoatState NONE = new BoatState(List.of(), List.of(), NEVER, NEVER);

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
            Deco.CODEC.listOf().optionalFieldOf("decos", List.of()).forGetter(BoatState::decos),
            Codec.LONG.optionalFieldOf("last_shot", NEVER).forGetter(BoatState::lastShot),
            Codec.LONG.optionalFieldOf("pet_lock_until", NEVER).forGetter(BoatState::petLockUntil)
    ).apply(instance, BoatState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoatState> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), BoatState::slots,
            Deco.STREAM_CODEC.apply(ByteBufCodecs.list()), BoatState::decos,
            ByteBufCodecs.VAR_LONG, BoatState::lastShot,
            ByteBufCodecs.VAR_LONG, BoatState::petLockUntil,
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

    public Optional<Deco> deco(BoatPartType type) {
        return decos.stream().filter(deco -> deco.type().equals(type.id())).findFirst();
    }

    public boolean sail() {
        return deco(BoatParts.SAIL).isPresent();
    }

    public boolean stern() {
        return deco(BoatParts.STERN).isPresent();
    }

    /** The slot the sail's mast belongs to, or -1. */
    public int sailSlot() {
        return deco(BoatParts.SAIL).map(Deco::slot).orElse(-1);
    }

    /** The sail: the banner on its item (empty while the mast is bare). */
    public ItemStack banner() {
        return deco(BoatParts.SAIL)
                .map(d -> {
                    BoatUpgrades.Sail sail = d.stack().get(BoatUpgrades.SAIL.get());
                    return sail == null ? ItemStack.EMPTY : sail.banner();
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
        return new BoatState(List.copyOf(list), decos, lastShot, petLockUntil);
    }

    /** This decoration in place of any of the same kind. */
    public BoatState withDeco(Deco deco) {
        List<Deco> list = new ArrayList<>(decos.stream().filter(other -> !other.type().equals(deco.type())).toList());
        list.add(deco);
        return new BoatState(slots, List.copyOf(list), lastShot, petLockUntil);
    }

    public BoatState withBanner(ItemStack banner) {
        Optional<Deco> sail = deco(BoatParts.SAIL);
        if (sail.isEmpty()) {
            return this;
        }
        ItemStack stack = sail.get().stack().copy();
        stack.set(BoatUpgrades.SAIL.get(), new BoatUpgrades.Sail(banner));
        return withDeco(new Deco(sail.get().type(), sail.get().slot(), stack));
    }

    public BoatState withShot(long tick) {
        return new BoatState(slots, decos, tick, petLockUntil);
    }

    public BoatState withPetLock(long untilTick) {
        return new BoatState(slots, decos, lastShot, untilTick);
    }

    /** Ticks until the cannon can be loaded again (0 = ready). */
    public int cooldown(long gameTime) {
        return (int) Math.max(0L, Cannon.COOLDOWN_TICKS - (gameTime - lastShot));
    }

    // ItemStack has no equals of its own - without these a state read back from disk would never equal the default.
    @Override
    public boolean equals(Object other) {
        return other instanceof BoatState state && slots.equals(state.slots) && decos.equals(state.decos)
                && lastShot == state.lastShot && petLockUntil == state.petLockUntil;
    }

    @Override
    public int hashCode() {
        return 31 * slots.hashCode() + Long.hashCode(lastShot);
    }
}
