package com.villageroverhaul.traderework.librarian;

import com.mojang.serialization.Codec;
import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.network.OpenLoreScrollPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A lore scroll (Obsidian Librarian.md, "Lore-Scrolls"): one item per origin and tier, found in structure
 * chests and some mob/block drops (data/villageroverhaul/loot_modifiers, via NeoForge's global loot
 * modifiers - no code) and handed in for Librarian quests.
 *
 * Right-click opens a book window with a lore fragment (client/ui/LoreScrollScreens). Which fragment is
 * fixed per scroll: the first read picks one of FRAGMENTS_PER_SCROLL at random on the server and stores
 * it in LORE_FRAGMENT, so the same scroll always shows the same text. The scroll is not used up.
 * Fragment texts are translation keys lore.villageroverhaul.<origin>_<tier>.<n> in the lang files.
 */
public class LoreScrollItem extends Item {

    public enum Origin implements StringRepresentable {
        OVERWORLD("overworld"),
        CAVE("cave"),
        UNDERWATER("underwater"),
        NETHER("nether");

        private final String serializedName;

        Origin(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final int TIERS = 3;
    /** How many different fragments each origin+tier has (lang keys .1 to .N) - Tweak-Werte.md. */
    public static final int FRAGMENTS_PER_SCROLL = 3;

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, TradeReworkMod.MODID);

    /** Which fragment (1..FRAGMENTS_PER_SCROLL) this scroll shows; absent until first read. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LORE_FRAGMENT =
            DATA_COMPONENTS.registerComponentType("lore_fragment",
                    builder -> builder.persistent(Codec.intRange(1, 255)).networkSynchronized(ByteBufCodecs.VAR_INT));

    private final Origin origin;
    private final int tier;

    public LoreScrollItem(Properties properties, Origin origin, int tier) {
        super(properties);
        this.origin = origin;
        this.tier = tier;
    }

    public static String id(Origin origin, int tier) {
        return origin.getSerializedName() + "_lore_scroll_" + tier;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            Integer fragment = stack.get(LORE_FRAGMENT.get());
            if (fragment == null) {
                fragment = 1 + level.getRandom().nextInt(FRAGMENTS_PER_SCROLL);
                stack.set(LORE_FRAGMENT.get(), fragment);
            }
            PacketDistributor.sendToPlayer(serverPlayer, new OpenLoreScrollPayload(origin.getSerializedName(), tier, fragment));
        }
        return InteractionResult.SUCCESS;
    }
}
