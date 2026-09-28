package com.villageroverhaul.world;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Brewing without a trip to the Nether (2026-09-28, emerald economy - weakness potions to cure zombie
 * villagers): a brewing stand generated in a village - the temples of all five village types have one - starts
 * with MIN_POWDER..MAX_POWDER blaze powder in its fuel slot.
 *
 * When: once, the first time a chunk is fully generated (ChunkEvent.Load with isNewChunk) - a chunk that
 * belongs to a village (it references a structure tagged #minecraft:village) is checked for brewing stands.
 * Only its block entities are looked at, never its blocks. Chunks generated before this existed stay as they are.
 *
 * NeoForge forbids touching the level while that event runs (the chunk isn't FULL yet), so the stands are only
 * noted there and filled on the next server tick - only if their fuel slot is still empty.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class VillageBrewingStands {

    /** Blaze powder per village brewing stand, evenly MIN..MAX (Tweak-Werte.md). */
    public static final int MIN_POWDER = 1;
    public static final int MAX_POWDER = 3;
    /** BrewingStandBlockEntity.FUEL_SLOT (private there). */
    private static final int FUEL_SLOT = 4;

    private static final Queue<GlobalPos> PENDING = new ConcurrentLinkedQueue<>();

    private VillageBrewingStands() {
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        LevelChunk chunk = event.getChunk();
        if (!event.isNewChunk() || !(chunk.getLevel() instanceof ServerLevel level) || !inVillage(level, chunk)) {
            return;
        }
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof BrewingStandBlockEntity) {
                PENDING.add(GlobalPos.of(level.dimension(), blockEntity.getBlockPos()));
            }
        }
    }

    private static boolean inVillage(ServerLevel level, LevelChunk chunk) {
        Registry<Structure> structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        return chunk.getAllReferences().keySet().stream().anyMatch(structure -> structures.wrapAsHolder(structure).is(StructureTags.VILLAGE));
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        GlobalPos pos;
        while ((pos = PENDING.poll()) != null) {
            ServerLevel level = event.getServer().getLevel(pos.dimension());
            if (level == null || !level.isLoaded(pos.pos())) {
                continue;
            }
            if (level.getBlockEntity(pos.pos()) instanceof BrewingStandBlockEntity stand && stand.getItem(FUEL_SLOT).isEmpty()) {
                int count = MIN_POWDER + level.getRandom().nextInt(MAX_POWDER - MIN_POWDER + 1);
                stand.setItem(FUEL_SLOT, new ItemStack(Items.BLAZE_POWDER, count));
            }
        }
    }
}
