package com.villageroverhaul.traderework.cage;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Zombie Villager Cage and everything it needs to be found by Iron Golems: the block, its item, the block entity and the point of interest.
 *
 * The POI covers only the free cage (lower half, not occupied): Vanilla keeps the point of interest in step with the block state by itself, so the
 * golem's search (PoiManager) never lists a full cage. It has no tickets (like the beehive) - who may deliver is the cage's own reservation
 * (ZombieVillagerCageBlockEntity.reserve), which is not saved and so cannot get stuck when a golem dies on the way.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class Cages {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TradeReworkMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TradeReworkMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TradeReworkMod.MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, TradeReworkMod.MODID);

    public static final DeferredBlock<ZombieVillagerCageBlock> CAGE = BLOCKS.registerBlock("zombie_villager_cage", ZombieVillagerCageBlock::new,
            properties -> properties.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.IRON).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> CAGE_ITEM = ITEMS.registerSimpleBlockItem(CAGE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZombieVillagerCageBlockEntity>> CAGE_ENTITY = BLOCK_ENTITY_TYPES.register(
            "zombie_villager_cage", () -> new BlockEntityType<>(ZombieVillagerCageBlockEntity::new, CAGE.get()));

    public static final DeferredHolder<PoiType, PoiType> CAGE_POI = POI_TYPES.register("zombie_villager_cage", () -> new PoiType(freeCageStates(), 0, 1));

    private Cages() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        POI_TYPES.register(modEventBus);
    }

    /** The states the golems look for: the lower half of a cage with nobody in it. */
    private static Set<BlockState> freeCageStates() {
        return CAGE.get().getStateDefinition().getPossibleStates().stream()
                .filter(state -> state.getValue(ZombieVillagerCageBlock.HALF) == DoubleBlockHalf.LOWER
                        && !state.getValue(ZombieVillagerCageBlock.OCCUPIED))
                .collect(Collectors.toUnmodifiableSet());
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CAGE_ITEM);
        }
    }
}
