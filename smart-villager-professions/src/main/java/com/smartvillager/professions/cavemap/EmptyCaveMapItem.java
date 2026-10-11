package com.smartvillager.professions.cavemap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.Optional;

/**
 * The empty Cave Map / Nether Map (2026-10-10). Right-click inside a layer of its dimension (CaveMapKind.usableLayer) uses it up and
 * hands out a filled map bound to that layer, centered at the player; anywhere else - the surface, the other dimension, the End -
 * nothing happens and the map is kept, so it is never wasted. Scale 0 (1 block per pixel), the player's marker is tracked.
 */
public class EmptyCaveMapItem extends Item {

    private final CaveMapKind kind;

    public EmptyCaveMapItem(Properties properties, CaveMapKind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockPos head = BlockPos.containing(player.getEyePosition());
        Optional<CaveLayer> layer = kind.usableLayer(level, head);
        if (layer.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        MapItemSavedData data = MapItemSavedData.createFresh(player.getX(), player.getZ(), (byte) 0, true, false, level.dimension());
        MapId id = serverLevel.getFreeMapId();
        serverLevel.setMapData(id, data);
        ItemStack filled = new ItemStack(CaveMapItems.filledOf(kind).get());
        filled.set(DataComponents.MAP_ID, id);
        filled.set(CaveMapItems.CAVE_LAYER.get(), layer.get());
        // The name tells the depth once the map is revealed: "Shallow Cave Map" (top layer) or "Deep Cave Map" (bottom layer).
        filled.set(DataComponents.ITEM_NAME, Component.translatable(CaveMapItems.filledOf(kind).get().getDescriptionId() + "." + layer.get().getSerializedName()));

        player.awardStat(Stats.ITEM_USED.get(this));
        level.playSound(null, player, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, player.getSoundSource(), 1.0F, 1.0F);
        CaveMapDrawer.draw(serverLevel, player, id, data, kind, layer.get());
        return InteractionResult.SUCCESS.heldItemTransformedTo(ItemUtils.createFilledResult(stack, player, filled));
    }
}
