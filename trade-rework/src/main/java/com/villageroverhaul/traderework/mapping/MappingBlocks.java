package com.villageroverhaul.traderework.mapping;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The two map-drawing passive stations (2026-10-10): the Cartographer's adventure station (adventure pool) and the Veteran's challenge
 * station (challenge pool). Both are MapStationBlock with a MapStationBlockEntity, they differ only in their pool (MapPool.Kind).
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class MappingBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradeReworkMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeReworkMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TradeReworkMod.MODID);

    public static final DeferredBlock<MapStationBlock> ADVENTURE_STATION = BLOCKS.registerBlock("adventure_station",
            properties -> new MapStationBlock(properties, MapPool.Kind.ADVENTURE),
            properties -> properties.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD));
    public static final DeferredBlock<MapStationBlock> CHALLENGE_STATION = BLOCKS.registerBlock("challenge_station",
            properties -> new MapStationBlock(properties, MapPool.Kind.CHALLENGE),
            properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.5F).sound(SoundType.DEEPSLATE));

    public static final DeferredItem<BlockItem> ADVENTURE_STATION_ITEM = ITEMS.registerSimpleBlockItem(ADVENTURE_STATION);
    public static final DeferredItem<BlockItem> CHALLENGE_STATION_ITEM = ITEMS.registerSimpleBlockItem(CHALLENGE_STATION);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MapStationBlockEntity>> ADVENTURE_STATION_ENTITY =
            BLOCK_ENTITY_TYPES.register("adventure_station", () -> new BlockEntityType<>(
                    (pos, state) -> new MapStationBlockEntity(MappingBlocks.ADVENTURE_STATION_ENTITY.get(), pos, state, MapPool.Kind.ADVENTURE),
                    ADVENTURE_STATION.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MapStationBlockEntity>> CHALLENGE_STATION_ENTITY =
            BLOCK_ENTITY_TYPES.register("challenge_station", () -> new BlockEntityType<>(
                    (pos, state) -> new MapStationBlockEntity(MappingBlocks.CHALLENGE_STATION_ENTITY.get(), pos, state, MapPool.Kind.CHALLENGE),
                    CHALLENGE_STATION.get()));

    private MappingBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }

    public static BlockEntityType<MapStationBlockEntity> blockEntityType(MapPool.Kind kind) {
        return (kind == MapPool.Kind.ADVENTURE ? ADVENTURE_STATION_ENTITY : CHALLENGE_STATION_ENTITY).get();
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ADVENTURE_STATION_ITEM);
            event.accept(CHALLENGE_STATION_ITEM);
        }
    }
}
