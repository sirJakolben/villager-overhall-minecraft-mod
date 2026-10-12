package com.villageroverhaul.api;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

/**
 * Extra data an extension attaches to an entry (data/ItemExchange) under "extensions": {"<id>": {...}}, read
 * back with exchange.extension(TYPE). Register with ExtensionHooks.registerExchangeExtension. The core never
 * looks inside - only the extension's own logic does, except for a value that is a ResultOverride.
 *
 * The exchange registry is synced to the client with the same codec, so both sides must register the type.
 * An entry naming an unregistered type fails to load (a typo shows at once) - a data pack for an extension
 * that may be missing guards its entries with NeoForge's "neoforge:conditions" (mod_loaded).
 */
public record ExchangeExtensionType<T>(Identifier id, Codec<T> codec) {

    /** The value as this type's T - only called with a value decoded by this type's codec. */
    @SuppressWarnings("unchecked")
    public T cast(Object value) {
        return (T) value;
    }
}
