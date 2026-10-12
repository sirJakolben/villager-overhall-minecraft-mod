package com.smartvillager.professions.boat;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * A kind of boat part (Obsidian Fisherman.md, 2026-10-12). SLOT parts (chest, cannon) fill one of the boat's slots - a slot is a seat position, so a
 * filled slot is a seat less. DECO parts (the mast) belong to a slot without filling it; a boat has at most one. The part's item is what the
 * Fisherman sells, what is built in with Shift + right click, and what drops when the boat is destroyed.
 * Other mods register their own kinds with BoatParts.register; the renderer draws the ones it knows (BoatUpgradeRenderer).
 */
public record BoatPartType(Identifier id, Kind kind, Supplier<? extends Item> item) {

    public enum Kind {
        SLOT, DECO
    }
}
