package com.villageroverhaul.core;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.core.state.VillagerState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, VillagerOverhaulMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<VillagerState>> VILLAGER_STATE = ATTACHMENT_TYPES.register(
            "villager_state",
            () -> AttachmentType.builder(VillagerState::initial)
                    .serialize(VillagerState.MAP_CODEC)
                    .sync(VillagerState.STREAM_CODEC)
                    .build()
    );

    private ModAttachments() {
    }
}
