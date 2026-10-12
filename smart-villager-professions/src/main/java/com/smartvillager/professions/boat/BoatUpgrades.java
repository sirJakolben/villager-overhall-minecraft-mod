package com.smartvillager.professions.boat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.fisherman.FishermanItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Boat parts and the boat's built-in state (Obsidian Fisherman.md, 2026-10-12). The Fisherman sells the parts as items (Boat Chest, Boat Cannon, Boat Mast);
 * Shift + right click on a boat builds one into a slot (BoatEvents) and it lives in the boat entity's synced attachment (BoatState) until the boat is
 * destroyed - then everything drops as items (BoatDrops). The mast item carries its banner (MAST) - the banner the player paid for it (api/ResultTransform
 * -> transform below); a boat's sail is that banner.
 */
public final class BoatUpgrades {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SmartVillagerProfessionsMod.MODID);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SmartVillagerProfessionsMod.MODID);

    /** On the mast item (and in the boat's deco item): the banner that is its sail (empty = bare mast). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Mast>> MAST =
            DATA_COMPONENTS.registerComponentType("boat_mast", builder -> builder.persistent(Mast.CODEC).networkSynchronized(Mast.STREAM_CODEC));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<BoatState>> STATE = ATTACHMENT_TYPES.register(
            "boat_state",
            () -> AttachmentType.builder(() -> BoatState.NONE).serialize(BoatState.MAP_CODEC).sync(BoatState.STREAM_CODEC).build()
    );

    public record Mast(ItemStack banner) {
        public static final Codec<Mast> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.OPTIONAL_CODEC.fieldOf("banner").forGetter(Mast::banner)
        ).apply(instance, Mast::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Mast> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC.map(Mast::new, Mast::banner);

        @Override
        public boolean equals(Object other) {
            return other instanceof Mast mast && ItemStack.matches(banner, mast.banner);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(banner);
        }
    }

    private BoatUpgrades() {
    }

    public static BoatState state(AbstractBoat boat) {
        return boat.getData(STATE);
    }

    public static void setState(AbstractBoat boat, BoatState state) {
        if (!state.equals(state(boat))) {
            boat.setData(STATE, state);
        }
    }

    /**
     * api/ResultTransform for the mast offer: the offer's result is a plain mast, what comes out is a mast carrying the banner the player put in.
     * Every other offer passes through.
     */
    public static ItemStack transform(MerchantOffer offer, ItemStack paymentA, ItemStack paymentB, ItemStack result) {
        if (!result.is(FishermanItems.BOAT_MAST.get())) {
            return result;
        }
        ItemStack banner = paymentA.is(ItemTags.BANNERS) ? paymentA : paymentB.is(ItemTags.BANNERS) ? paymentB : ItemStack.EMPTY;
        if (banner.isEmpty()) {
            return result;
        }
        ItemStack mast = result.copy();
        mast.set(MAST.get(), new Mast(banner.copyWithCount(1)));
        return mast;
    }
}
