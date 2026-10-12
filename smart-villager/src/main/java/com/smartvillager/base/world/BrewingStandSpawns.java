package com.smartvillager.base.world;

import com.smartvillager.base.SmartVillagerMod;
import com.smartvillager.base.mixin.BrewingStandAccessor;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Brewing without a trip to the Nether: every brewing stand that world generation places (village temples, swamp huts, igloo basements, ...)
 * starts with FUEL_MIN..FUEL_MAX of its 20 fuel charges, and with BLAZE_POWDER_CHANCE one blaze powder waiting in its fuel slot.
 *
 * When: once, the first time a chunk is fully generated (ChunkEvent.Load with isNewChunk). Only its block entities are looked at, never its
 * blocks. Chunks generated before this existed stay as they are, and so does every stand a player places.
 *
 * NeoForge forbids touching the level while that event runs (the chunk isn't FULL yet), so the stands are only noted there and filled on the
 * next server tick.
 */
@EventBusSubscriber(modid = SmartVillagerMod.MODID)
public final class BrewingStandSpawns {

    /** Vanilla's full fuel is 20 charges (one blaze powder); a generated stand holds half to all of it (Tweak-Werte.md). */
    public static final int FUEL_MAX = 20;
    public static final int FUEL_MIN = 10;
    /** Chance for one blaze powder in the fuel slot, on top of the fuel charges. */
    public static final float BLAZE_POWDER_CHANCE = 0.5F;
    /** BrewingStandBlockEntity.FUEL_SLOT (private there). */
    private static final int FUEL_SLOT = 4;

    private static final Queue<GlobalPos> PENDING = new ConcurrentLinkedQueue<>();

    private BrewingStandSpawns() {
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        LevelChunk chunk = event.getChunk();
        if (!event.isNewChunk() || !(chunk.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof BrewingStandBlockEntity) {
                PENDING.add(GlobalPos.of(level.dimension(), blockEntity.getBlockPos()));
            }
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        GlobalPos pos;
        while ((pos = PENDING.poll()) != null) {
            ServerLevel level = event.getServer().getLevel(pos.dimension());
            if (level == null || !level.isLoaded(pos.pos())) {
                continue;
            }
            if (level.getBlockEntity(pos.pos()) instanceof BrewingStandBlockEntity stand) {
                fill(level, stand);
            }
        }
    }

    /** Fuel charges and the maybe-blaze-powder for one freshly generated stand (also called by the gametests). */
    public static void fill(ServerLevel level, BrewingStandBlockEntity stand) {
        ((BrewingStandAccessor) stand).smartvillager$setFuel(FUEL_MIN + level.getRandom().nextInt(FUEL_MAX - FUEL_MIN + 1));
        if (stand.getItem(FUEL_SLOT).isEmpty() && level.getRandom().nextFloat() < BLAZE_POWDER_CHANCE) {
            stand.setItem(FUEL_SLOT, new ItemStack(Items.BLAZE_POWDER));
        }
        stand.setChanged();
    }
}
