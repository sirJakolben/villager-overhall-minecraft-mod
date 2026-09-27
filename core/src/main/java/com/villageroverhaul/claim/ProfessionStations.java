package com.villageroverhaul.claim;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.core.state.StationSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.poi.ExtendPoiTypesEvent;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Each profession's three workplaces (decided 2026-09-24: all three are equal job sites). The master
 * and passive stations are added to the profession's Vanilla job-site type through NeoForge's
 * ExtendPoiTypesEvent - so to Vanilla they simply are lecterns (or stonecutters): a jobless villager becomes a Librarian
 * at any of them, any of them is enough to keep the job and to have a work time, and Vanilla validates
 * them like a lectern. No mixin needed. Which of the three a given job site is, is told apart by its
 * block (slotAt).
 *
 * The core itself has no stations of its own (split 2026-09-28): the Trade Rework extension registers its
 * professions' stations through api/ExtensionHooks.registerStations. A profession without an entry here
 * runs on the non-implementation rule (fallback/FallbackCatalog).
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class ProfessionStations {

    public record Entry(ResourceKey<VillagerProfession> profession, ResourceKey<PoiType> jobSite,
                        Supplier<? extends Block> master, Supplier<? extends Block> passive) {

        /** For a profession without that station yet (Veteran, Cartographer: no passive station, 2026-09-27). */
        public static final Supplier<Block> NO_STATION = () -> null;

        /** Which slot the job site at pos is: its master or passive station, or the Vanilla block (basic). */
        public StationSlot slotOf(Block block) {
            if (block == master.get()) {
                return StationSlot.MASTER;
            }
            return block == passive.get() && block != null ? StationSlot.PASSIVE : StationSlot.BASIC;
        }
    }

    /** Filled from extension mod constructors (possibly in parallel), read ever after. */
    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private ProfessionStations() {
    }

    /** Use api/ExtensionHooks.registerStations. */
    public static void register(Entry entry) {
        ENTRIES.add(entry);
    }

    @SubscribeEvent
    static void onExtendPoiTypes(ExtendPoiTypesEvent event) {
        for (Entry entry : ENTRIES) {
            for (Supplier<? extends Block> station : List.of(entry.master(), entry.passive())) {
                if (station.get() != null) {
                    event.addBlockToPoi(entry.jobSite(), station.get());
                }
            }
        }
    }

    public static Optional<Entry> of(Holder<VillagerProfession> profession) {
        return ENTRIES.stream().filter(entry -> profession.is(entry.profession())).findFirst();
    }

    public static StationSlot slotAt(ServerLevel level, Entry entry, BlockPos pos) {
        return entry.slotOf(level.getBlockState(pos).getBlock());
    }
}
