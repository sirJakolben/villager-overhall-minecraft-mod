package com.villageroverhaul.menu;

import com.villageroverhaul.api.ExtensionHooks;
import com.villageroverhaul.section.RowView;
import com.villageroverhaul.section.SectionView;
import com.villageroverhaul.section.VillagerSections;
import com.villageroverhaul.trade.RequiredEnchantmentCost;
import com.villageroverhaul.work.RestockService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Optional;

/**
 * Vanilla's trading machinery in our own layout. The container (MerchantContainer) and result slot
 * (MerchantResultSlot, extended only by a player-XP price) are Vanilla's classes - they decide when a result appears,
 * enforce out-of-stock, shrink payment by the (discounted) cost and call Merchant.notifyTrade.
 * The Merchant is the villager itself on the server and Vanilla's ClientSideMerchant on the client,
 * exactly like MerchantMenu; the client's copy of the offers arrives via VillagerOffersPayload.
 *
 * Only the methods that live on MerchantMenu itself (tryMoveItems, moveFromInventoryToPaymentSlot,
 * quickMoveStack, removed) are carried over, because MerchantMenu can't be extended: its constructor
 * hard-codes Vanilla's slot positions and Slot.x/y are final. The only difference to MerchantMenu is the
 * slot positions, from the villager_gui.png measurement: payment boxes at panel-relative (179,37) and
 * (205,37); result box (259,33) 24x24 with the 16x16 icon centered in it.
 */
public class VillagerMenu extends AbstractContainerMenu {

    public static final int GRID_LEFT = 151;
    public static final int GRID_TOP = 84;
    public static final int GRID_COLUMNS = 9;
    public static final int GRID_WIDTH = GRID_COLUMNS * 18;

    private static final int PAYMENT_A_CONTAINER_SLOT = 0;
    private static final int PAYMENT_B_CONTAINER_SLOT = 1;
    private static final int RESULT_CONTAINER_SLOT = 2;
    private static final int PAYMENT_A_X = 179;
    private static final int PAYMENT_B_X = 205;
    private static final int PAYMENT_Y = 37;
    private static final int RESULT_X = 259 + 4;
    private static final int RESULT_Y = 33 + 4;

    private static final int PAYMENT_A_SLOT = 0;
    private static final int PAYMENT_B_SLOT = 1;
    private static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_END = 39;

    private final Villager villager;
    private final Merchant trader;
    private final MerchantContainer tradeContainer;
    private List<SectionView> sections = List.of();
    private List<RowView> rows = List.of();
    private int offersVersion;

    /** Server side - the villager is the Merchant. */
    public VillagerMenu(int containerId, Inventory inventory, Villager villager) {
        this(containerId, inventory, villager, villager);
    }

    /** Client side - offers are filled in later by VillagerOffersPayload, like Vanilla's ClientboundMerchantOffersPacket. */
    public VillagerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, resolveVillager(inventory, data.readVarInt()), new ClientSideMerchant(inventory.player));
    }

    private VillagerMenu(int containerId, Inventory inventory, Villager villager, Merchant trader) {
        super(ModMenuTypes.VILLAGER_MENU.get(), containerId);
        this.villager = villager;
        this.trader = trader;
        this.tradeContainer = new MerchantContainer(trader);
        addSlot(new Slot(tradeContainer, PAYMENT_A_CONTAINER_SLOT, PAYMENT_A_X, PAYMENT_Y));
        addSlot(new Slot(tradeContainer, PAYMENT_B_CONTAINER_SLOT, PAYMENT_B_X, PAYMENT_Y));
        addSlot(new PlayerXpCostResultSlot(inventory.player, trader, RESULT_X, RESULT_Y));
        addStandardInventorySlots(inventory, GRID_LEFT, GRID_TOP);
    }

    private static Villager resolveVillager(Inventory inventory, int entityId) {
        Entity entity = inventory.player.level().getEntity(entityId);
        if (entity instanceof Villager villager) {
            return villager;
        }
        throw new IllegalStateException("Villager menu opened for entity id " + entityId + ", which is not a villager");
    }

    public Villager villager() {
        return villager;
    }

    public MerchantOffers getOffers() {
        return trader.getOffers();
    }

    /** The open sections in order, as the server sent them - the offer list holds their rows one after another. */
    public List<SectionView> sections() {
        return sections;
    }

    /** Per row, what the offer itself doesn't carry (section/RowView); empty for a row the server didn't describe. */
    public Optional<RowView> row(int index) {
        return index >= 0 && index < rows.size() ? Optional.of(rows.get(index)) : Optional.empty();
    }

    /** Bumped on every client-side offers update, so the screen knows when to rebuild its rows. */
    public int offersVersion() {
        return offersVersion;
    }

    /** The three item icons of an offer row. */
    public enum OfferItem {
        COST_A, COST_B, RESULT;

        public ItemStack of(MerchantOffer offer) {
            return switch (this) {
                case COST_A -> offer.getCostA();
                case COST_B -> offer.getCostB();
                case RESULT -> offer.getResult();
            };
        }
    }

    /**
     * Creative middle-click on a row's item icon (Vanilla's CLONE on a slot): a full stack of it on the
     * cursor, only with an empty cursor and only for players with infinite materials. Run by the client
     * for prediction and by the server (CloneOfferItemPayload). The cost marker of an enchantment
     * requirement never ends up on a real item.
     */
    public void cloneOfferItem(Player player, int index, OfferItem item) {
        MerchantOffers offers = getOffers();
        if (!player.hasInfiniteMaterials() || !getCarried().isEmpty() || index < 0 || index >= offers.size()) {
            return;
        }
        ItemStack stack = item.of(offers.get(index));
        if (stack.isEmpty()) {
            return;
        }
        ItemStack clone = stack.copyWithCount(stack.getMaxStackSize());
        clone.remove(RequiredEnchantmentCost.REQUIRES_ENCHANTMENT.get());
        setCarried(clone);
    }

    public void setOffers(MerchantOffers offers, List<SectionView> sections, List<RowView> rows) {
        trader.overrideOffers(offers);
        this.sections = sections;
        this.rows = rows;
        this.offersVersion++;
        refreshActiveOffer();
    }

    /** Re-resolves the result slot against the current offer list - after a rebuild, the old offer objects are gone. */
    public void refreshActiveOffer() {
        tradeContainer.updateSellItem();
    }

    public void setSelectionHint(int hint) {
        tradeContainer.setSelectionHint(hint);
    }

    @Override
    public void slotsChanged(Container container) {
        tradeContainer.updateSellItem();
        super.slotsChanged(container);
    }

    /** Runs every server tick while the menu is open: sections update their time-based state (a paused quest row reappears) and a held meter is released on time. */
    @Override
    public void broadcastChanges() {
        VillagerSections.refresh(villager);
        RestockService.releaseDue(villager);
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return trader.stillValid(player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return false;
    }

    // Everything below mirrors MerchantMenu 1:1.

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex == RESULT_SLOT) {
                // Vanilla's shift-click loop only checks mayPickup before its first iteration - re-check
                // each time, so bottling stops once the player's XP runs out.
                if (!slot.mayPickup(player)) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
                playTradeSound();
            } else if (slotIndex != PAYMENT_A_SLOT && slotIndex != PAYMENT_B_SLOT) {
                if (slotIndex < INV_SLOT_END) {
                    if (!this.moveItemStackTo(stack, INV_SLOT_END, USE_ROW_SLOT_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, INV_SLOT_START, USE_ROW_SLOT_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return clicked;
    }

    private void playTradeSound() {
        if (!trader.isClientSide()) {
            villager.level().playLocalSound(villager.getX(), villager.getY(), villager.getZ(), trader.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        trader.setTradingPlayer(null);
        if (!trader.isClientSide()) {
            ItemStack paymentA = tradeContainer.removeItemNoUpdate(PAYMENT_A_CONTAINER_SLOT);
            ItemStack paymentB = tradeContainer.removeItemNoUpdate(PAYMENT_B_CONTAINER_SLOT);
            if (!player.isAlive() || player instanceof ServerPlayer serverPlayer && serverPlayer.hasDisconnected()) {
                if (!paymentA.isEmpty()) {
                    player.drop(paymentA, false);
                }
                if (!paymentB.isEmpty()) {
                    player.drop(paymentB, false);
                }
            } else if (player instanceof ServerPlayer) {
                player.getInventory().placeItemBackInInventory(paymentA);
                player.getInventory().placeItemBackInInventory(paymentB);
            }
        }
    }

    /**
     * Vanilla's MerchantResultSlot plus one extra price: a trade can also cost the player XP
     * (api/ExtensionHooks.playerXpCost - Trade Rework: Bottles o' Enchanting cost the bottle's XP; 0 in the core). The result stays visible but
     * can't be taken without enough XP; creative players bottle for free, like the enchanting table.
     */
    private final class PlayerXpCostResultSlot extends MerchantResultSlot {
        PlayerXpCostResultSlot(Player player, Merchant merchant, int x, int y) {
            super(player, merchant, tradeContainer, RESULT_CONTAINER_SLOT, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            int cost = ExtensionHooks.playerXpCost(tradeContainer.getActiveOffer());
            return cost == 0 || player.hasInfiniteMaterials() || totalXpPoints(player) >= cost;
        }

        @Override
        public void onTake(Player player, ItemStack carried) {
            int cost = ExtensionHooks.playerXpCost(tradeContainer.getActiveOffer());
            super.onTake(player, carried);
            // Both sides, so the client's shift-click prediction stops at the same bottle as the
            // server; the server's authoritative XP sync overwrites the client value afterwards anyway.
            if (cost > 0 && !player.hasInfiniteMaterials()) {
                player.giveExperiencePoints(-cost);
            }
        }
    }

    /**
     * The player's actual XP points, rebuilt from level + progress bar with Vanilla's own per-level
     * cost. Player.totalExperience isn't used because Vanilla doesn't lower it when levels are spent
     * (enchanting, anvils) - it only counts XP ever collected.
     */
    public static int totalXpPoints(Player player) {
        int points = 0;
        for (int level = 0; level < player.experienceLevel; level++) {
            points += xpNeededForNextLevel(level);
        }
        return points + Math.round(player.experienceProgress * xpNeededForNextLevel(player.experienceLevel));
    }

    /** Same formula as Player.getXpNeededForNextLevel, for an arbitrary level. */
    private static int xpNeededForNextLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    public void tryMoveItems(int newTradeIndex) {
        if (newTradeIndex >= 0 && getOffers().size() > newTradeIndex) {
            ItemStack oldCostA = tradeContainer.getItem(PAYMENT_A_CONTAINER_SLOT);
            if (!oldCostA.isEmpty()) {
                if (!this.moveItemStackTo(oldCostA, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    return;
                }
                tradeContainer.setItem(PAYMENT_A_CONTAINER_SLOT, oldCostA);
            }

            ItemStack oldCostB = tradeContainer.getItem(PAYMENT_B_CONTAINER_SLOT);
            if (!oldCostB.isEmpty()) {
                if (!this.moveItemStackTo(oldCostB, INV_SLOT_START, USE_ROW_SLOT_END, true)) {
                    return;
                }
                tradeContainer.setItem(PAYMENT_B_CONTAINER_SLOT, oldCostB);
            }

            if (tradeContainer.getItem(PAYMENT_A_CONTAINER_SLOT).isEmpty() && tradeContainer.getItem(PAYMENT_B_CONTAINER_SLOT).isEmpty()) {
                MerchantOffer offer = getOffers().get(newTradeIndex);
                moveFromInventoryToPaymentSlot(PAYMENT_A_CONTAINER_SLOT, offer.getItemCostA());
                offer.getItemCostB().ifPresent(cost -> moveFromInventoryToPaymentSlot(PAYMENT_B_CONTAINER_SLOT, cost));
            }
        }
    }

    private void moveFromInventoryToPaymentSlot(int paymentSlot, ItemCost cost) {
        for (int i = INV_SLOT_START; i < USE_ROW_SLOT_END; i++) {
            ItemStack inventoryItem = this.slots.get(i).getItem();
            if (!inventoryItem.isEmpty() && cost.test(inventoryItem)) {
                ItemStack currentPaymentItem = tradeContainer.getItem(paymentSlot);
                if (currentPaymentItem.isEmpty() || ItemStack.isSameItemSameComponents(inventoryItem, currentPaymentItem)) {
                    int maxStackSize = inventoryItem.getMaxStackSize();
                    int moveCount = Math.min(maxStackSize - currentPaymentItem.getCount(), inventoryItem.getCount());
                    ItemStack newPaymentItem = inventoryItem.copyWithCount(currentPaymentItem.getCount() + moveCount);
                    inventoryItem.shrink(moveCount);
                    tradeContainer.setItem(paymentSlot, newPaymentItem);
                    if (newPaymentItem.getCount() >= maxStackSize) {
                        break;
                    }
                }
            }
        }
    }
}
