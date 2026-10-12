package com.villageroverhaul.traderework.mason;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Only there so the client can draw the statue with a block entity renderer (client/render/VillagerStatueRenderer),
 * like Vanilla's copper golem statue - it holds no data, biome and figure live in the block and its state.
 * Only the lower half of a statue has one.
 */
public class VillagerStatueBlockEntity extends BlockEntity {

    public VillagerStatueBlockEntity(BlockPos pos, BlockState state) {
        super(MasonBlockEntities.VILLAGER_STATUE.get(), pos, state);
    }
}
