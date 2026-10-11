package com.smartvillager.professions.boat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Boat upgrades (Obsidian Fisherman.md, 2026-10-12). A cannon or a mast lives in two places: on the boat ITEM as data components (CANNON,
 * MAST - so the boat keeps its wood and the upgrades while carried, traded and dropped) and on the boat ENTITY as one synced attachment
 * (BoatState). Placing the item copies the components into the attachment (mixin/BoatItemMixin), destroying the boat copies them back onto
 * the dropped item (mixin/VehicleEntityDropMixin) - so the upgrades are permanent. They are applied in the trade window: the Fisherman's
 * upgrade offers pay with the player's own boat of any wood (an item-tag payment, progression trade/TagCost) and the result slot returns
 * that very boat upgraded (api/ResultTransform -> transform below).
 */
public final class BoatUpgrades {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SmartVillagerProfessionsMod.MODID);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SmartVillagerProfessionsMod.MODID);

    /** On a boat item: it carries a cannon. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> CANNON =
            DATA_COMPONENTS.registerComponentType("boat_cannon", builder -> builder.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    /** On a boat item: it carries a mast, with this banner as its sail (empty = bare mast). */
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

    /** A new boat entity placed from this item takes over its upgrades. */
    public static void applyFromItem(AbstractBoat boat, ItemStack stack) {
        boolean cannon = stack.has(CANNON.get());
        Mast mast = stack.get(MAST.get());
        if (cannon || mast != null) {
            setState(boat, new BoatState(cannon, mast != null, mast != null ? mast.banner().copy() : ItemStack.EMPTY, BoatState.NEVER));
        }
    }

    /** The dropped item of a destroyed boat gets its upgrades back. */
    public static void applyToItem(AbstractBoat boat, ItemStack stack) {
        BoatState state = state(boat);
        if (state.cannon()) {
            stack.set(CANNON.get(), Unit.INSTANCE);
        }
        if (state.mast()) {
            stack.set(MAST.get(), new Mast(state.banner().copy()));
        }
    }

    /**
     * api/ResultTransform for the upgrade offers: their result stack (BoatUpgradeEntry) only names which upgrade it is; what comes out is
     * the player's own boat, one of it, with that upgrade added - a new mast starts with a white banner.
     */
    public static ItemStack transform(MerchantOffer offer, ItemStack paymentA, ItemStack paymentB, ItemStack result) {
        boolean cannon = result.has(CANNON.get());
        boolean mast = result.has(MAST.get());
        if (!cannon && !mast) {
            return result;
        }
        ItemStack boat = paymentA.is(ItemTags.BOATS) ? paymentA : paymentB.is(ItemTags.BOATS) ? paymentB : ItemStack.EMPTY;
        if (boat.isEmpty()) {
            return result;
        }
        ItemStack upgraded = boat.copyWithCount(1);
        if (cannon) {
            upgraded.set(CANNON.get(), Unit.INSTANCE);
        }
        if (mast && !upgraded.has(MAST.get())) {
            upgraded.set(MAST.get(), new Mast(new ItemStack(Items.WHITE_BANNER)));
        }
        return upgraded;
    }
}
