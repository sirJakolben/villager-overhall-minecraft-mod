package com.villageroverhaul.claim;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.cartographer.CartographerBlocks;
import com.villageroverhaul.core.state.StationSlot;
import com.villageroverhaul.librarian.LibrarianBlocks;
import com.villageroverhaul.mason.MasonBlocks;
import com.villageroverhaul.runesmith.RunesmithBlocks;
import com.villageroverhaul.salvager.SalvagerBlocks;
import com.villageroverhaul.veteran.VeteranBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.poi.ExtendPoiTypesEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Each profession's three workplaces (decided 2026-09-24: all three are equal job sites). The master
 * and passive stations are added to the profession's Vanilla job-site type through NeoForge's
 * ExtendPoiTypesEvent - so to Vanilla they simply are lecterns (or stonecutters): a jobless villager becomes a Librarian
 * at any of them, any of them is enough to keep the job and to have a work time, and Vanilla validates
 * them like a lectern. No mixin needed. Which of the three a given job site is, is told apart by its
 * block (slotAt).
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

    private static final List<Entry> ENTRIES = List.of(
            new Entry(VillagerProfession.LIBRARIAN, PoiTypes.LIBRARIAN, LibrarianBlocks.WRITING_STATION, LibrarianBlocks.ENCHANTMENT_STATION),
            new Entry(VillagerProfession.MASON, PoiTypes.MASON, MasonBlocks.METAMORPH_STATION, MasonBlocks.CRUSHING_STATION),
            new Entry(VillagerProfession.TOOLSMITH, PoiTypes.TOOLSMITH, RunesmithBlocks.UPGRADE_STATION, RunesmithBlocks.REPAIR_STATION),
            new Entry(VillagerProfession.WEAPONSMITH, PoiTypes.WEAPONSMITH, VeteranBlocks.CHALLENGE_STATION, Entry.NO_STATION),
            new Entry(VillagerProfession.CARTOGRAPHER, PoiTypes.CARTOGRAPHER, CartographerBlocks.EXPLORE_STATION, Entry.NO_STATION),
            new Entry(VillagerProfession.ARMORER, PoiTypes.ARMORER, SalvagerBlocks.REFINEMENT_STATION, SalvagerBlocks.SMELTING_STATION)
    );

    private ProfessionStations() {
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
