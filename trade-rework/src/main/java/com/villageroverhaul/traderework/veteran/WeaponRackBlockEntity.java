package com.villageroverhaul.traderework.veteran;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What hangs on a weapon rack (WeaponRackBlock): one weapon or nothing. Saved with the block and sent to
 * clients for the renderer. Which items count as weapons is an item tag (WEAPONS: swords, axes, spears,
 * trident, mace, bow, crossbow) so datapacks and other mods can add theirs.
 */
public class WeaponRackBlockEntity extends BlockEntity {

    public static final TagKey<Item> WEAPONS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "weapon_rack_weapons"));

    private ItemStack weapon = ItemStack.EMPTY;

    public WeaponRackBlockEntity(BlockPos pos, BlockState state) {
        super(VeteranBlockEntities.WEAPON_RACK.get(), pos, state);
    }

    public static boolean fits(ItemStack stack) {
        return stack.is(WEAPONS);
    }

    public ItemStack weapon() {
        return weapon;
    }

    public void hang(ItemStack stack) {
        weapon = stack;
        changed();
    }

    public ItemStack take() {
        ItemStack taken = weapon;
        weapon = ItemStack.EMPTY;
        changed();
        return taken;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !weapon.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), weapon);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        weapon = input.read("weapon", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!weapon.isEmpty()) {
            output.store("weapon", ItemStack.CODEC, weapon);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
