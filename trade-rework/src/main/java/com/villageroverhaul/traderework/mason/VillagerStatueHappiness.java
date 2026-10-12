package com.villageroverhaul.traderework.mason;

import com.villageroverhaul.api.HappinessElement;
import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.stream.Collectors;

/**
 * Villager statues make villagers happier (2026-09-28): one or more statues within RADIUS blocks of a villager
 * count as a happiness element of 10 % with a 2-day timer, like the bell (core api/HappinessElement). More statues
 * don't add more.
 *
 * To find them cheaply the statues are points of interest (VILLAGER_STATUE, only the lower half that has the
 * figure) - the villager's scan asks the POI index around it instead of scanning blocks, and only until it saw a
 * statue that day. No villager ever claims them (0 tickets).
 */
public final class VillagerStatueHappiness {

    /** Happiness share of statues nearby, in percent (Tweak-Werte.md). */
    public static final int WEIGHT = 10;
    /** Days until the share has faded to 0 after the last statue sighting (Tweak-Werte.md) - same as the bell. */
    public static final int TIMER_DAYS = 2;
    /** How close a statue must be, in blocks (Tweak-Werte.md) - same as the bell. */
    public static final int RADIUS = 8;

    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, TradeReworkMod.MODID);

    public static final DeferredHolder<PoiType, PoiType> VILLAGER_STATUE = POI_TYPES.register("villager_statue",
            () -> new PoiType(MasonBlocks.VILLAGER_STATUES.values().stream()
                    .flatMap(block -> block.get().getStateDefinition().getPossibleStates().stream())
                    .filter(state -> state.getValue(VillagerStatueBlock.HALF) == DoubleBlockHalf.LOWER)
                    .collect(Collectors.toUnmodifiableSet()), 0, 1));

    public static final HappinessElement ELEMENT = new HappinessElement(
            Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "villager_statue"), WEIGHT, TIMER_DAYS,
            (level, villager) -> level.getPoiManager()
                    .getInRange(poi -> poi.is(VILLAGER_STATUE.getKey()), villager.blockPosition(), RADIUS, PoiManager.Occupancy.ANY)
                    .findAny()
                    .isPresent());

    private VillagerStatueHappiness() {
    }
}
