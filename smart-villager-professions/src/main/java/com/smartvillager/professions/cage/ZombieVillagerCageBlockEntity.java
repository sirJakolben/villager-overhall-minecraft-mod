package com.smartvillager.professions.cage;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * What the Zombie Villager Cage holds, and the cure.
 *
 * The prisoner is only data: the zombie villager's saved entity tag (like a bee in a hive), so nothing of it ticks, attacks or burns. Taking it in
 * (insert) saves and removes the mob; letting it out (releaseOccupant) loads it back - also when the cage is broken, burnt or pushed away
 * (preRemoveSideEffects). The client gets the tag too and draws a display copy (client/CageRenderer).
 *
 * The cure is Vanilla's: a potion of weakness (any form) weakens the prisoner for WEAKNESS_TICKS, a golden apple on a weakened prisoner starts the
 * cure (Vanilla's 3 to 5 minutes). When it is done the cage opens and the prisoner is let out as a zombie villager with the conversion at its last
 * tick - Vanilla's own conversion makes the villager (data, trades, gossip, reputation, advancement). Breaking the cage in the middle of the
 * cure lets it out with the rest of the time still running.
 *
 * The reservation (which golem is bringing a zombie villager here) lives only in memory.
 */
public class ZombieVillagerCageBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** How long the potion of weakness weakens the prisoner (ticks; Vanilla's potion lasts 90 s, a splash potion 67 s). */
    public static final int WEAKNESS_TICKS = 2400;
    /** The cure takes this long, plus up to {@link #CURE_EXTRA_TICKS} (Vanilla: 3600 + 0 to 2400). */
    public static final int CURE_MIN_TICKS = 3600;
    public static final int CURE_EXTRA_TICKS = 2400;
    /** A golem keeps the cage for this long without renewing (ticks). */
    public static final int RESERVATION_TICKS = 400;

    private @Nullable CompoundTag occupant;
    private long weakenedUntil = -1L;
    private long cureAt = -1L;
    private @Nullable UUID curer;

    private @Nullable UUID reservedBy;
    private long reservedUntil;

    /** Client only: the display copy of the prisoner, built from the tag by the renderer. */
    private @Nullable Entity displayEntity;

    public ZombieVillagerCageBlockEntity(BlockPos pos, BlockState state) {
        super(Cages.CAGE_ENTITY.get(), pos, state);
    }

    public boolean isOccupied() {
        return occupant != null;
    }

    public boolean isCuring() {
        return occupant != null && cureAt >= 0L;
    }

    public @Nullable CompoundTag occupantTag() {
        return occupant;
    }

    public @Nullable Entity displayEntity() {
        return displayEntity;
    }

    public void setDisplayEntity(@Nullable Entity displayEntity) {
        this.displayEntity = displayEntity;
    }

    /* ---- the reservation (server) ---- */

    public boolean isFreeFor(UUID golem, long now) {
        return occupant == null && (reservedBy == null || reservedBy.equals(golem) || now >= reservedUntil);
    }

    /** True if the golem now holds the cage (or renewed its hold). */
    public boolean reserve(UUID golem, long now) {
        if (!isFreeFor(golem, now)) {
            return false;
        }
        reservedBy = golem;
        reservedUntil = now + RESERVATION_TICKS;
        return true;
    }

    public void unreserve(UUID golem) {
        if (golem.equals(reservedBy)) {
            reservedBy = null;
        }
    }

    /* ---- taking in ---- */

    /** The golem's delivery: saves the zombie villager into the cage and removes the mob. False if the cage is not free. */
    public boolean insert(ZombieVillager zombie) {
        if (!(level instanceof ServerLevel server) || occupant != null || zombie.level() != server) {
            return false;
        }
        zombie.stopRiding();
        CompoundTag tag;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(zombie.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, zombie.registryAccess());
            zombie.save(output);
            tag = output.buildResult();
        }
        // A new identity and no place, movement or fire to come back with.
        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("Fire");
        zombie.discard();
        occupant = tag;
        weakenedUntil = -1L;
        cureAt = -1L;
        curer = null;
        reservedBy = null;
        ZombieVillagerCageBlock.setOccupied(server, worldPosition, true);
        changed();
        server.playSound(null, worldPosition, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.8F);
        return true;
    }

    /* ---- the cure ---- */

    /** Right-click with a potion of weakness or a golden apple. */
    public InteractionResult interact(ItemStack stack, Player player, InteractionHand hand) {
        if (occupant == null || isCuring()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level instanceof ServerLevel server)) {
            // The client may say yes already; the server decides.
            return isWeaknessPotion(stack) || stack.is(Items.GOLDEN_APPLE) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        long now = server.getGameTime();
        if (isWeaknessPotion(stack)) {
            weakenedUntil = now + WEAKNESS_TICKS;
            if (stack.is(Items.POTION)) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
            } else {
                stack.consume(1, player);
            }
            server.playSound(null, worldPosition, SoundEvents.SPLASH_POTION_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            server.sendParticles(ParticleTypes.WITCH, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, 14, 0.3, 0.6, 0.3, 0.02);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(Items.GOLDEN_APPLE)) {
            if (now >= weakenedUntil) {
                // Like Vanilla: it only works on a weakened zombie villager, and the apple stays.
                return InteractionResult.CONSUME;
            }
            stack.consume(1, player);
            startCure(server, player.getUUID(), now + CURE_MIN_TICKS + server.getRandom().nextInt(CURE_EXTRA_TICKS + 1));
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private void startCure(ServerLevel server, UUID starter, long finishAt) {
        cureAt = finishAt;
        curer = starter;
        weakenedUntil = -1L;
        // The display copy shakes like a curing zombie villager does.
        if (occupant != null) {
            occupant.putInt("ConversionTime", (int) Math.max(1L, finishAt - server.getGameTime()));
        }
        server.scheduleTick(worldPosition, getBlockState().getBlock(), (int) Math.max(1L, finishAt - server.getGameTime()));
        server.playSound(null, worldPosition, SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.BLOCKS, 1.0F, 1.0F);
        changed();
    }

    /** The scheduled tick at the end of the cure: the cage opens and the villager comes out. */
    public void finishCureIfDue(ServerLevel server) {
        if (occupant == null || cureAt < 0L) {
            return;
        }
        if (server.getGameTime() < cureAt) {
            server.scheduleTick(worldPosition, getBlockState().getBlock(), (int) (cureAt - server.getGameTime()));
            return;
        }
        releaseOccupant(server, findExit(server), true);
        server.playSound(null, worldPosition, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static boolean isWeaknessPotion(ItemStack stack) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.is(MobEffects.WEAKNESS)) {
                return true;
            }
        }
        return false;
    }

    /* ---- letting out ---- */

    /** A free place next to the cage, else the cage's own block. */
    private Vec3 findExit(ServerLevel server) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Vec3 spot = new Vec3(worldPosition.getX() + 0.5 + direction.getStepX(), worldPosition.getY(), worldPosition.getZ() + 0.5 + direction.getStepZ());
            AABB box = EntityType.ZOMBIE_VILLAGER.getDimensions().makeBoundingBox(spot);
            if (server.noCollision(box)) {
                return spot;
            }
        }
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
    }

    /** Loads the prisoner back into the world at the spot; during a cure the conversion keeps running (cured = at its last tick). */
    private void releaseOccupant(ServerLevel server, Vec3 spot, boolean cured) {
        CompoundTag tag = occupant;
        if (tag == null) {
            return;
        }
        tag = tag.copy();
        if (cureAt >= 0L) {
            tag.putInt("ConversionTime", cured ? 1 : (int) Math.max(1L, cureAt - server.getGameTime()));
            if (curer != null) {
                tag.store("ConversionPlayer", UUIDUtil.CODEC, curer);
            }
        }
        occupant = null;
        weakenedUntil = -1L;
        cureAt = -1L;
        curer = null;
        Entity zombie = EntityType.loadEntityRecursive(EntityType.ZOMBIE_VILLAGER, tag, server, EntitySpawnReason.LOAD, EntityProcessor.NOP);
        if (zombie != null) {
            zombie.snapTo(spot.x, spot.y, spot.z, server.getRandom().nextFloat() * 360.0F, 0.0F);
            if (zombie instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            server.addFreshEntity(zombie);
        }
        ZombieVillagerCageBlock.setOccupied(server, worldPosition, false);
        changed();
    }

    /** The cage goes away (broken, blown up, pushed): the prisoner is let out in its place. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && occupant != null) {
            releaseOccupant(server, new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), false);
        }
    }

    /* ---- saving and syncing ---- */

    private void changed() {
        setChanged();
        displayEntity = null;
        Level level = this.level;
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("Occupant", CompoundTag.CODEC, occupant);
        output.putLong("WeakenedUntil", weakenedUntil);
        output.putLong("CureAt", cureAt);
        output.storeNullable("Curer", UUIDUtil.CODEC, curer);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        occupant = input.read("Occupant", CompoundTag.CODEC).orElse(null);
        weakenedUntil = input.getLongOr("WeakenedUntil", -1L);
        cureAt = input.getLongOr("CureAt", -1L);
        curer = input.read("Curer", UUIDUtil.CODEC).orElse(null);
        displayEntity = null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
