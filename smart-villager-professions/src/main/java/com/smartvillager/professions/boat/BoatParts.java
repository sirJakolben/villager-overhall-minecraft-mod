package com.smartvillager.professions.boat;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.fisherman.FishermanItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** The known boat part kinds - ours (chest, cannon, mast) and whatever other mods register. */
public final class BoatParts {

    private static final Map<Identifier, BoatPartType> TYPES = new ConcurrentHashMap<>();

    /** Marks a free slot in BoatState. */
    public static final Identifier EMPTY = id("empty");

    public static final BoatPartType CHEST = register(new BoatPartType(id("chest"), BoatPartType.Kind.SLOT, FishermanItems.BOAT_CHEST));
    /** Only ever in the frontmost slot (BoatSlots.canInstall). */
    public static final BoatPartType CANNON = register(new BoatPartType(id("cannon"), BoatPartType.Kind.SLOT, FishermanItems.BOAT_CANNON));
    public static final BoatPartType MAST = register(new BoatPartType(id("mast"), BoatPartType.Kind.DECO, FishermanItems.BOAT_MAST));

    private BoatParts() {
    }

    public static BoatPartType register(BoatPartType type) {
        TYPES.put(type.id(), type);
        return type;
    }

    public static Optional<BoatPartType> get(Identifier id) {
        return Optional.ofNullable(TYPES.get(id));
    }

    /** The part kind a held item installs, if it is one. */
    public static Optional<BoatPartType> ofItem(ItemStack stack) {
        Item item = stack.getItem();
        return TYPES.values().stream().filter(type -> type.item().get() == item).findFirst();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, path);
    }
}
