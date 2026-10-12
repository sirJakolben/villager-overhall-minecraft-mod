package com.smartvillager.professions.boat;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * What the boat's chests hold (Obsidian Fisherman.md, 2026-10-12): 27 slots per chest, so two chests are one double chest of 54 (the menu stops at six
 * rows, so more chests than two add no room). Server only - the menu syncs it to whoever has it open; which chests are built in is BoatState.
 * The contents are written back to the attachment on every change, so they survive a reload and are there when the boat is destroyed (BoatDrops).
 */
public final class BoatInventory {

    private static final int SLOTS_PER_CHEST = 27;
    private static final int MAX_ROWS = 6;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SmartVillagerProfessionsMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemContainerContents>> CONTENTS = ATTACHMENT_TYPES.register(
            "boat_inventory",
            () -> AttachmentType.builder(() -> ItemContainerContents.EMPTY).serialize(ItemContainerContents.CODEC.fieldOf("items")).build()
    );

    private BoatInventory() {
    }

    public static ItemContainerContents contents(AbstractBoat boat) {
        return boat.getData(CONTENTS);
    }

    /** Opens the chest menu of a boat that has at least one chest built in. */
    public static void open(ServerPlayer player, AbstractBoat boat) {
        int chests = BoatUpgrades.state(boat).chests();
        if (chests == 0) {
            return;
        }
        int rows = Math.min(MAX_ROWS, 3 * chests);
        SimpleContainer container = new SimpleContainer(rows * 9) {
            @Override
            public boolean stillValid(Player viewer) {
                return boat.isAlive() && viewer.distanceToSqr(boat) < 64.0;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                boat.setData(CONTENTS, ItemContainerContents.fromItems(getItems()));
            }
        };
        NonNullList<ItemStack> stored = NonNullList.withSize(Math.max(contents(boat).getSlots(), container.getContainerSize()), ItemStack.EMPTY);
        contents(boat).copyInto(stored);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            container.setItem(slot, stored.get(slot));
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, viewer) -> rows == 3 ? ChestMenu.threeRows(id, inventory, container) : ChestMenu.sixRows(id, inventory, container),
                Component.translatable("container.smartvillager_professions.boat_chest")));
    }

    /** Every stack in the chests, for BoatDrops. */
    public static Iterable<ItemStack> stacks(AbstractBoat boat) {
        return contents(boat).nonEmptyItemCopyStream().toList();
    }
}
