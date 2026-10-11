package com.smartvillager.professions.cavemap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A filled Cave Map / Nether Map (2026-10-10): a normal Vanilla map (map id, marker, sync) whose pixels do not come from the surface but
 * from what the holder sees in the cave (CaveMapDrawer). It is bound to the layer it was made in (CAVE_LAYER) and draws only while its holder
 * stands in that layer - the rest of the time the picture just stays.
 */
public class CaveMapItem extends MapItem {

    private final CaveMapKind kind;

    public CaveMapItem(Properties properties, CaveMapKind kind) {
        super(properties);
        this.kind = kind;
    }

    /** Vanilla would draw the surface: the cave map draws from inventoryTick instead. */
    @Override
    public void update(Level level, Entity player, MapItemSavedData data) {
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        MapItemSavedData data = getSavedData(stack, level);
        if (data == null || !(owner instanceof Player player)) {
            return;
        }
        data.tickCarriedBy(player, stack, null);
        CaveLayer layer = stack.get(CaveMapItems.CAVE_LAYER.get());
        MapId id = stack.get(DataComponents.MAP_ID);
        if (!data.locked && layer != null && id != null && slot != null && slot.getType() == EquipmentSlot.Type.HAND
                && CaveMapDrawer.isDue(level, player)) {
            CaveMapDrawer.draw(level, player, id, data, kind, layer);
        }
    }

    /**
     * Zooming out works at the cartography table with paper, as for any map (it only asks for a map id). Vanilla would start the bigger map
     * empty; here the picture and the pixel memory move over (CaveMapDrawer.migrate), so nothing explored is lost. Locking stays Vanilla's.
     */
    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        if (stack.get(DataComponents.MAP_POST_PROCESSING) == MapPostProcessing.SCALE && level instanceof ServerLevel serverLevel) {
            stack.remove(DataComponents.MAP_POST_PROCESSING);
            MapId oldId = stack.get(DataComponents.MAP_ID);
            MapItemSavedData old = getSavedData(stack, serverLevel);
            if (oldId != null && old != null) {
                MapItemSavedData zoomed = old.scaled();
                MapId newId = serverLevel.getFreeMapId();
                serverLevel.setMapData(newId, zoomed);
                CaveMapDrawer.migrate(serverLevel, kind, oldId, old, newId, zoomed);
                stack.set(DataComponents.MAP_ID, newId);
            }
        } else {
            super.onCraftedPostProcess(stack, level);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        CaveLayer layer = stack.get(CaveMapItems.CAVE_LAYER.get());
        if (layer != null) {
            builder.accept(Component.translatable("cave_map.layer", Component.translatable("cave_layer." + (kind == CaveMapKind.NETHER ? "nether." : "") + layer.getSerializedName()))
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
