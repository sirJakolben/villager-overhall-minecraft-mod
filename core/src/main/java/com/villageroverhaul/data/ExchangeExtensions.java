package com.villageroverhaul.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.villageroverhaul.api.ExchangeExtensionType;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An entry's "extensions" (api/ExchangeExtensionType): one value per registered type, keyed by the type's id in
 * the JSON. The registered types are filled from mod constructors (possibly in parallel) and only read after that.
 */
public record ExchangeExtensions(Map<ExchangeExtensionType<?>, Object> values) {

    public static final ExchangeExtensions NONE = new ExchangeExtensions(Map.of());

    private static final Map<Identifier, ExchangeExtensionType<?>> TYPES = new ConcurrentHashMap<>();

    private static final Codec<ExchangeExtensionType<?>> TYPE_CODEC = Identifier.CODEC.comapFlatMap(
            id -> Optional.<ExchangeExtensionType<?>>ofNullable(TYPES.get(id)).map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown entry extension " + id + " - is the mod that adds it installed?")),
            ExchangeExtensionType::id);

    public static final Codec<ExchangeExtensions> CODEC = Codec.<ExchangeExtensionType<?>, Object>dispatchedMap(TYPE_CODEC, ExchangeExtensionType::codec)
            .xmap(values -> new ExchangeExtensions(Map.copyOf(values)), ExchangeExtensions::values);

    public static void register(ExchangeExtensionType<?> type) {
        if (TYPES.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("Entry extension registered twice: " + type.id());
        }
    }

    public <T> Optional<T> get(ExchangeExtensionType<T> type) {
        return Optional.ofNullable(values.get(type)).map(type::cast);
    }
}
