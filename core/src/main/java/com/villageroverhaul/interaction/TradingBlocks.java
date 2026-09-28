package com.villageroverhaul.interaction;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Trading Block (2026-09-24): a frame with a crossed staff, model from
 * assets/trading_block/trading_block.bbmodel. A villager gets assigned to one with
 * an emerald (ManualClaims); while the block is powered, or for a short visit after a right-click
 * (TradingBlock), that villager walks there (TradingBlockCall). The villager remembers its block, not
 * the other way round.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class TradingBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VillagerOverhaulMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VillagerOverhaulMod.MODID);

    public static final DeferredBlock<TradingBlock> TRADING_BLOCK = BLOCKS.registerBlock("trading_block", TradingBlock::new,
            properties -> properties.strength(2.5F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> TRADING_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(TRADING_BLOCK);

    private TradingBlocks() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TRADING_BLOCK_ITEM);
        }
    }
}
