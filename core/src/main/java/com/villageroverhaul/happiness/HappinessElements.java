package com.villageroverhaul.happiness;

import com.villageroverhaul.api.HappinessElement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** The extra happiness elements extensions registered (api/HappinessElement), in registration order. */
public final class HappinessElements {

    private static final List<HappinessElement> ELEMENTS = new CopyOnWriteArrayList<>();

    private HappinessElements() {
    }

    public static void register(HappinessElement element) {
        if (ELEMENTS.stream().anyMatch(existing -> existing.id().equals(element.id()))) {
            throw new IllegalStateException("Happiness element " + element.id() + " registered twice");
        }
        ELEMENTS.add(element);
    }

    public static List<HappinessElement> all() {
        return ELEMENTS;
    }
}
