package com.smartvillager.base.trading;

import com.smartvillager.base.SmartVillagerMod;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;

/**
 * The Trading Block a villager was assigned to - the villager remembers its block, not the other way round. A Data Attachment of its own
 * (server only, saved with the villager), so Smart Villager keeps working without any other mod.
 *
 * Survives a zombie conversion and back, like the rest of what the villager owns: it is no reservation, and TradingBlockUpkeep drops it
 * if it no longer fits.
 */
@EventBusSubscriber(modid = SmartVillagerMod.MODID)
public final class TradingBlockLinks {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SmartVillagerMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Optional<GlobalPos>>> LINK = ATTACHMENT_TYPES.register(
            "trading_block",
            () -> AttachmentType.<Optional<GlobalPos>>builder(Optional::empty)
                    .serialize(GlobalPos.CODEC.optionalFieldOf("pos"))
                    .build()
    );

    private TradingBlockLinks() {
    }

    public static Optional<GlobalPos> get(Entity villager) {
        return villager.hasData(LINK) ? villager.getData(LINK) : Optional.empty();
    }

    public static void set(Entity villager, Optional<GlobalPos> pos) {
        if (pos.isPresent() || villager.hasData(LINK)) {
            villager.setData(LINK, pos);
        }
    }

    @SubscribeEvent
    static void onConvert(LivingConversionEvent.Post event) {
        LivingEntity from = event.getEntity();
        LivingEntity to = event.getOutcome();
        if (!from.level().isClientSide() && isVillagerLike(from) && isVillagerLike(to)) {
            set(to, get(from));
        }
    }

    private static boolean isVillagerLike(LivingEntity entity) {
        return entity instanceof Villager || entity instanceof ZombieVillager;
    }
}
