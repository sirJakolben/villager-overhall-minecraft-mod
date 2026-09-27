package com.villageroverhaul.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.villageroverhaul.trade.MissingTrade;
import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.StationSlot;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.trade.RestockService;
import com.villageroverhaul.network.CloneOfferItemPayload;
import com.villageroverhaul.network.InvestUpgradePointPayload;
import com.villageroverhaul.network.RerollQuestPayload;
import com.villageroverhaul.network.SelectOfferPayload;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.progression.UpgradeGroup;
import com.villageroverhaul.quest.QuestActions;
import com.villageroverhaul.quest.QuestSlots;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Our layout (villager_gui.pxo, 2026-09-22 brainstorm) around Vanilla's trading behavior. Every row
 * reads straight from the menu's MerchantOffers, the same list MerchantScreen reads - so discount
 * strikethrough, out-of-stock arrow/X and item tooltips follow MerchantScreen's own drawing code
 * (extractAndDecorateCostA, extractButtonArrows, TradeOfferButton.extractToolTip). Clicking a row
 * runs setSelectionHint + tryMoveItems locally for prediction and sends SelectOfferPayload, like
 * MerchantScreen.postButtonClick.
 *
 * Sections come from the offer list's fixed order (quests, basic trades, master trades) with sizes
 * from VillagerOffersPayload. Rows are rebuilt whenever that payload or the synced VillagerState
 * changes. The basic/master meters show the productivity meters (RestockService), the happiness
 * meter the synced happiness (HappinessCalculator). Known first-pass simplification: the Passive
 * group's invest button placement is a judgment call. The list scrolls (wheel or scroller drag) once
 * it outgrows the panel. Quest rows have a reroll button right of the reward (back 2026-09-26); per-row
 * stock display is intentionally absent for now (2026-09-23).
 */
public class VillagerScreen extends AbstractContainerScreen<VillagerMenu> {

    private static final int LIST_X = 48;
    private static final int LIST_WIDTH = 88;
    private static final int SECTION_START_Y = 8;
    // Rows are 20px apart, the button height, so buttons touch. Each 21px trade_group_sides tile starts
    // with a 1px dark line; tiles are drawn 20px apart, so only the first line (the frame's top edge)
    // shows - the others sit under the button above. Buttons start below that line.
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_TOP_BORDER = 1;

    // Scrollable list viewport and scroll track, measured from villager_gui.png: the dark list area is
    // x 48-135 / y 8-157 (88x150), the strip right of it x 137-142 is Vanilla scroller width (6px).
    private static final int LIST_TOP = SECTION_START_Y;
    private static final int LIST_HEIGHT = 150;
    private static final int SCROLLER_X = 137;
    private static final int SCROLLER_WIDTH = 6;
    private static final int SCROLLER_HEIGHT = 27;
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/villager/scroller_disabled");
    // Screen-relative positions below are measured on the villager_gui.pxo canvas (layer_bounds_2026-09-24.json)
    // with the origin kept at canvas (63, 52), where the panel started before the 2026-09-24 redesign - so
    // every slot/list position stayed valid. The panel texture itself now starts PANEL_X to the right; the
    // passive ability group and the stat group sit behind it and stick out to the left.
    private static final int SCREEN_WIDTH = 319;
    private static final int PANEL_X = 41;
    private static final int PASSIVE_GROUP_X = -7;
    private static final int PASSIVE_GROUP_Y = 5;
    private static final int PASSIVE_BUTTON_X = 23;
    private static final int PASSIVE_BUTTON_Y = 8;
    // Stat group: left cap, one middle tile per visible meter, right piece (holds happiness) at a fixed spot.
    private static final int STAT_GROUP_RIGHT_X = 27;
    private static final int STAT_GROUP_Y = 71;
    // Happiness meter background (9x78) - also its tooltip hover area.
    private static final int HAPPINESS_BAR_X = 29;
    private static final int HAPPINESS_BAR_Y = 77;
    // Productivity meter backgrounds (11x79) sit 1px into their middle tile, the fill 3px into the background.
    private static final int METER_TILE_OFFSET = 1;
    private static final int METER_FILL_OFFSET = 3;
    private static final int METER_Y = 77;
    private static final int TITLE_COLOR = 0xFF404040;

    // Rows sit inside the trade_group frame, 3px in from each side - Vanilla's own 20px button height.
    private static final int ROW_BUTTON_INSET = 3;
    private static final int ROW_BUTTON_WIDTH = LIST_WIDTH - 2 * ROW_BUTTON_INSET;
    private static final int ROW_BUTTON_HEIGHT = 20;
    // Row-relative item positions, MerchantScreen's costA / costB / arrow / result squeezed into our 82px
    // row. The discounted count is drawn right of the costA icon (up to +31, see extractAndDecorateCost),
    // which is why costB starts that far right - as in Vanilla, the count may touch costB's edge.
    private static final int ROW_INPUT_X = 2;
    private static final int ROW_SECOND_INPUT_X = 32;
    private static final int ROW_ARROW_X = 50;
    private static final int ROW_RESULT_X = 63;
    private static final int ROW_ITEM_Y = 2;
    // Quest rows (2026-09-26): a quest never has a second payment, so arrow and reward move left and the
    // reroll button takes the room on the right. The arrow still clears the discounted count (up to +31).
    private static final int QUEST_ROW_ARROW_X = 34;
    private static final int QUEST_ROW_RESULT_X = 46;
    private static final int QUEST_ROW_REROLL_X = 65;
    private static final int QUEST_ROW_REROLL_Y = 2;
    private static final long TICKS_PER_DAY = 24000L;

    private static final Identifier XP_BAR_BACKGROUND = Identifier.withDefaultNamespace("container/villager/experience_bar_background");
    private static final Identifier XP_BAR_CURRENT = Identifier.withDefaultNamespace("container/villager/experience_bar_current");
    private static final Identifier OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/out_of_stock");
    private static final Identifier TRADE_ARROW_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow");
    private static final Identifier TRADE_ARROW_OUT_OF_STOCK_SPRITE = Identifier.withDefaultNamespace("container/villager/trade_arrow_out_of_stock");
    private static final Identifier DISCOUNT_STRIKETHROUGH_SPRITE = Identifier.withDefaultNamespace("container/villager/discount_strikethrough");
    private static final int XP_BAR_WIDTH = 102;
    private static final int XP_BAR_HEIGHT = 5;
    // The out_of_stock X (28x21) centered over the arrow between the second payment box and the result box (arrow x=229-250).
    private static final int OUT_OF_STOCK_X = 226;
    private static final int OUT_OF_STOCK_Y = 35;

    private static final int HEADER_BUTTON_X_ADJUST = -3;
    private static final int BUTTON_Y_ADJUST = 2;

    /** firstOffer is the index of this section's first row in the menu's full offer list. */
    private record SectionLayout(String title, int headerY, int contentY, int bottomY, int firstOffer, int rowCount) {
    }

    private final Villager villager;
    private final List<TradeOfferButton> tradeOfferButtons = new ArrayList<>();
    /** Quest rows' reroll buttons with the quest slot each one rerolls. */
    private final Map<RerollButtonWidget, Integer> rerollButtons = new HashMap<>();
    /** Row and section-header buttons: input via addWidget, drawn by hand under the list scissor. */
    private final List<AbstractWidget> listWidgets = new ArrayList<>();
    private List<SectionLayout> sectionLayouts = List.of();
    private VillagerState lastKnownState;
    private int lastKnownOffersVersion;
    private int selectedOffer = -1;
    /** Pixels the list content is shifted up; kept across rebuilds, re-clamped in init(). */
    private int scrollOffset;
    private int maxScroll;
    private boolean draggingScroller;

    public VillagerScreen(VillagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SCREEN_WIDTH, VillagerGuiTextures.PANEL.height());
        this.villager = menu.villager();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        VillagerState current = VillagerStateAccess.of(villager).getState();
        if (affectsWidgets(lastKnownState, current) || menu.offersVersion() != lastKnownOffersVersion) {
            rebuildWidgets();
        }
    }

    /**
     * Only what the buttons show (rank labels, upgrade-cost tooltips) - XP and happiness change every
     * few seconds while the villager works, but are drawn fresh each frame and need no rebuild.
     */
    private static boolean affectsWidgets(VillagerState before, VillagerState now) {
        return before == null
                || !before.ranks().equals(now.ranks())
                || before.unspentUpgradePoints() != now.unspentUpgradePoints()
                || !before.stations().equals(now.stations());
    }

    @Override
    protected void init() {
        super.init();
        listWidgets.clear();

        this.titleLabelX = VillagerMenu.GRID_LEFT;
        this.titleLabelY = 6;
        this.inventoryLabelX = VillagerMenu.GRID_LEFT;
        this.inventoryLabelY = VillagerMenu.GRID_TOP - 10;

        VillagerState state = VillagerStateAccess.of(villager).getState();
        lastKnownState = state;
        lastKnownOffersVersion = menu.offersVersion();

        int offerCount = menu.getOffers().size();
        int questCount = Math.min(menu.questCount(), offerCount);
        int basicCount = Math.min(menu.basicCount(), offerCount - questCount);
        int masterCount = offerCount - questCount - basicCount;

        List<SectionLayout> layouts = new ArrayList<>();
        int y = SECTION_START_Y;
        // Trades / Masteries / Passive only show while the villager owns that station or has a rank in the
        // group (ProgressionService.isGroupOpen, 2026-09-24); an open section shows even while empty, so
        // its first rank can be bought from its header.
        SectionLayout questSection = layoutSection(layouts, y, "Quests", 0, questCount);
        SectionLayout lastSection = questSection;
        SectionLayout tradeSection = null;
        if (basicCount > 0 || ProgressionService.isGroupOpen(state, UpgradeGroup.BASIC_TRADE)) {
            tradeSection = layoutSection(layouts, nextSectionY(lastSection), "Trades", questCount, basicCount);
            lastSection = tradeSection;
        }
        SectionLayout masterSection = masterCount > 0 || menu.rankCaps().isFallback() || ProgressionService.isGroupOpen(state, UpgradeGroup.MASTER_TRADE)
                ? layoutSection(layouts, nextSectionY(lastSection), "Masteries", questCount + basicCount, masterCount)
                : null;
        this.sectionLayouts = layouts;
        int contentBottom = nextSectionY(layouts.get(layouts.size() - 1));
        this.maxScroll = Math.max(0, contentBottom - LIST_TOP - LIST_HEIGHT);
        this.scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        addGroupHeaderButton(UpgradeGroup.QUEST, questSection.headerY(), state);
        if (tradeSection != null) {
            addGroupHeaderButton(UpgradeGroup.BASIC_TRADE, tradeSection.headerY(), state);
        }
        if (masterSection != null) {
            addGroupHeaderButton(UpgradeGroup.MASTER_TRADE, masterSection.headerY(), state);
        }

        // The whole passive group (backdrop, label, rank button) only shows while the villager owns a passive station.
        if (state.stations().passive().isPresent()) {
            addPassiveButton(state);
        }
        tradeOfferButtonsRebuild(layouts);
    }

    private void addPassiveButton(VillagerState state) {
        // Measured from the passive group's own trade_level_button layer in the pxo.
        int passiveButtonX = leftPos + PASSIVE_BUTTON_X;
        int passiveButtonY = topPos + PASSIVE_BUTTON_Y;
        addRenderableWidget(createGroupHeaderButton(UpgradeGroup.PASSIVE, passiveButtonX, passiveButtonY, state));
    }

    private void tradeOfferButtonsRebuild(List<SectionLayout> layouts) {
        tradeOfferButtons.clear();
        rerollButtons.clear();
        for (SectionLayout section : layouts) {
            for (int i = 0; i < section.rowCount(); i++) {
                int x = leftPos + LIST_X + ROW_BUTTON_INSET;
                int rowY = section.contentY() + ROW_TOP_BORDER + i * ROW_HEIGHT - scrollOffset;
                int index = section.firstOffer() + i;
                boolean visible = touchesList(rowY, ROW_BUTTON_HEIGHT);
                // Added before its row button, so a click on it doesn't select the row; drawn after it.
                RerollButtonWidget reroll = createRerollButton(index, x, topPos + rowY);
                if (reroll != null) {
                    reroll.visible = visible;
                    addWidget(reroll);
                }
                TradeOfferButton button = addWidget(new TradeOfferButton(x, topPos + rowY, index));
                button.visible = visible;
                button.active = !isEmptyRow(index);
                tradeOfferButtons.add(button);
                listWidgets.add(button);
                if (reroll != null) {
                    listWidgets.add(reroll);
                }
            }
        }
    }

    /** Every quest row except the permanent quest (it never rotates) gets a reroll button. */
    private RerollButtonWidget createRerollButton(int index, int rowX, int rowY) {
        List<Integer> questSlots = menu.questSlots();
        if (index >= questSlots.size() || QuestSlots.isPermanent(questSlots.get(index))) {
            return null;
        }
        int slot = questSlots.get(index);
        RerollButtonWidget button = new RerollButtonWidget(rowX + QUEST_ROW_REROLL_X, rowY + QUEST_ROW_REROLL_Y,
                () -> rerollCooldownTicks(slot) <= 0,
                () -> ClientPacketDistributor.sendToServer(new RerollQuestPayload(slot))) {
            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                return super.isMouseOver(mouseX, mouseY) && isInListViewport(mouseX, mouseY);
            }
        };
        rerollButtons.put(button, slot);
        return button;
    }

    private long rerollCooldownTicks(int slot) {
        return QuestActions.rerollCooldownTicksRemaining(VillagerStateAccess.of(villager).getState(), villager, slot);
    }

    /**
     * Ready: "Reroll quest". During the cooldown a day timer (2026-09-26): one diamond per day of the full
     * cooldown, a filled one per day still to wait - a 3-day cooldown shows ◆◆◆, then ◆◆◇, on the last
     * day ◆◇◇; a 2-day cooldown ◆◆, then ◆◇. The full cooldown is taken at the current quest rank (like
     * the one the reroll set), never shorter than the days actually left.
     */
    private Component rerollTooltip(int slot) {
        long remaining = rerollCooldownTicks(slot);
        if (remaining <= 0) {
            return Component.translatable("gui.villageroverhaul.quest_reroll");
        }
        int daysLeft = (int) ((remaining + TICKS_PER_DAY - 1) / TICKS_PER_DAY);
        int totalDays = Math.max(daysLeft, QuestActions.rerollCooldownDays(VillagerStateAccess.of(villager).getState()));
        return Component.literal("◆".repeat(daysLeft) + "◇".repeat(totalDays - daysLeft));
    }

    /**
     * List widgets are shown while any part of them is inside the list viewport; they are drawn under
     * the list's scissor (extractContents), so they slide behind the edge like the section frames.
     */
    private static boolean touchesList(int panelY, int height) {
        return panelY + height > LIST_TOP && panelY < LIST_TOP + LIST_HEIGHT;
    }

    /** Clicks on the clipped-away part of a list widget don't count. */
    private boolean isInListViewport(double mouseX, double mouseY) {
        return mouseY >= topPos + LIST_TOP && mouseY < topPos + LIST_TOP + LIST_HEIGHT
                && mouseX >= leftPos + LIST_X && mouseX < leftPos + LIST_X + LIST_WIDTH;
    }

    private void setScrollOffset(int offset) {
        int clamped = Mth.clamp(offset, 0, maxScroll);
        if (clamped != scrollOffset) {
            scrollOffset = clamped;
            rebuildWidgets();
        }
    }

    private boolean isOverList(double mouseX, double mouseY) {
        return mouseX >= leftPos + LIST_X && mouseX < leftPos + SCROLLER_X + SCROLLER_WIDTH
                && mouseY >= topPos + LIST_TOP && mouseY < topPos + LIST_TOP + LIST_HEIGHT;
    }

    // Scrolling mirrors MerchantScreen: mouse wheel, or dragging the scroller along its track.

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (maxScroll > 0 && isOverList(x, y)) {
            setScrollOffset(scrollOffset - (int) (scrollY * ROW_HEIGHT));
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (maxScroll > 0
                && event.x() >= leftPos + SCROLLER_X && event.x() < leftPos + SCROLLER_X + SCROLLER_WIDTH
                && event.y() >= topPos + LIST_TOP && event.y() < topPos + LIST_TOP + LIST_HEIGHT) {
            draggingScroller = true;
            dragScrollerTo(event.y());
            return true;
        }
        if (tryCloneOfferItem(event)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * Creative middle-click on a row's item icon: a full stack on the cursor, like Vanilla's CLONE on a
     * slot. Predicted locally, repeated by the server (CloneOfferItemPayload) - as in selectOffer.
     */
    private boolean tryCloneOfferItem(MouseButtonEvent event) {
        var mouseKey = InputConstants.Type.MOUSE.getOrCreate(event.button());
        if (!this.minecraft.options.keyPickItem.isActiveAndMatches(mouseKey) || !this.minecraft.player.hasInfiniteMaterials()) {
            return false;
        }
        MerchantOffers offers = menu.getOffers();
        for (TradeOfferButton button : tradeOfferButtons) {
            if (!button.visible || button.index >= offers.size() || !button.isMouseOver(event.x(), event.y())) {
                continue;
            }
            VillagerMenu.OfferItem item = button.itemAt(offers.get(button.index), event.x());
            if (item == null) {
                return false;
            }
            menu.cloneOfferItem(this.minecraft.player, button.index, item);
            ClientPacketDistributor.sendToServer(new CloneOfferItemPayload(button.index, item));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScroller) {
            dragScrollerTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroller = false;
        return super.mouseReleased(event);
    }

    private void dragScrollerTo(double mouseY) {
        float fraction = (float) (mouseY - topPos - LIST_TOP - SCROLLER_HEIGHT / 2.0) / (LIST_HEIGHT - SCROLLER_HEIGHT);
        setScrollOffset(Math.round(Mth.clamp(fraction, 0.0F, 1.0F) * maxScroll));
    }

    private SectionLayout layoutSection(List<SectionLayout> layouts, int headerY, String title, int firstOffer, int rowCount) {
        int contentY = headerY + VillagerGuiTextures.TRADE_GROUP_TOP.height();
        int bottomY = contentY + ROW_TOP_BORDER + Math.max(rowCount, 1) * ROW_HEIGHT;
        SectionLayout layout = new SectionLayout(title, headerY, contentY, bottomY, firstOffer, rowCount);
        layouts.add(layout);
        return layout;
    }

    private static int nextSectionY(SectionLayout previous) {
        return previous.bottomY() + VillagerGuiTextures.TRADE_GROUP_BOTTOM.height();
    }

    /**
     * A placeholder trade (trade/MissingTrade, 2026-09-27): a rank whose trade isn't designed yet. Its row is
     * drawn as an empty, disabled button - no cost, no arrow, no result, no tooltip, not selectable.
     */
    private boolean isEmptyRow(int index) {
        MerchantOffers offers = menu.getOffers();
        return index >= 0 && index < offers.size() && offers.get(index).getResult().is(MissingTrade.ITEM.get());
    }

    /** MerchantScreen.postButtonClick - predict locally, then let the server do the same authoritatively. */
    private void selectOffer(int index) {
        this.selectedOffer = index;
        menu.setSelectionHint(index);
        menu.tryMoveItems(index);
        ClientPacketDistributor.sendToServer(new SelectOfferPayload(index));
    }

    private void addGroupHeaderButton(UpgradeGroup group, int headerY, VillagerState state) {
        int x = leftPos + LIST_X + LIST_WIDTH - VillagerGuiTextures.TRADE_LEVEL_BUTTON.width() - 2 + HEADER_BUTTON_X_ADJUST;
        int panelY = headerY - scrollOffset + (VillagerGuiTextures.TRADE_GROUP_TOP.height() - VillagerGuiTextures.TRADE_LEVEL_BUTTON.height()) / 2 + BUTTON_Y_ADJUST;
        TradeLevelButtonWidget button = addWidget(createGroupHeaderButton(group, x, topPos + panelY, state));
        button.visible = touchesList(panelY, VillagerGuiTextures.TRADE_LEVEL_BUTTON.height());
        listWidgets.add(button);
    }

    private TradeLevelButtonWidget createGroupHeaderButton(UpgradeGroup group, int x, int y, VillagerState state) {
        int rank = ProgressionService.rankOf(state.ranks(), group);
        Runnable invest = () -> ClientPacketDistributor.sendToServer(new InvestUpgradePointPayload(group));
        TradeLevelButtonWidget button = group == UpgradeGroup.PASSIVE
                ? new TradeLevelButtonWidget(x, y, String.valueOf(rank), invest)
                : new TradeLevelButtonWidget(x, y, String.valueOf(rank), invest) {
                    @Override
                    public boolean isMouseOver(double mouseX, double mouseY) {
                        return super.isMouseOver(mouseX, mouseY) && isInListViewport(mouseX, mouseY);
                    }
                };
        int cost = menu.rankCaps().upgradeCost(group, state.ranks());
        button.setTooltip(Tooltip.create(cost < 0
                ? Component.translatable("gui.villageroverhaul.upgrade.maxed")
                : Component.translatable("gui.villageroverhaul.upgrade.cost", cost, state.unspentUpgradePoints())));
        button.setShowArrow(cost >= 0 && cost <= state.unspentUpgradePoints());
        return button;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        VillagerState state = VillagerStateAccess.of(villager).getState();
        int tier = ProgressionService.merchantTier(state.level());
        Component titleAndTier = Component.translatable("merchant.title", this.title, Component.translatable("merchant.level." + tier));

        // extractLabels already runs translated by (leftPos, topPos) - panel-relative coords only.
        int titleWidth = this.font.width(titleAndTier);
        int titleX = VillagerMenu.GRID_LEFT + (VillagerMenu.GRID_WIDTH - titleWidth) / 2;
        graphics.text(this.font, titleAndTier, titleX, this.titleLabelY, TITLE_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TITLE_COLOR, false);

        if (tier < 5) {
            int barX = VillagerMenu.GRID_LEFT + (VillagerMenu.GRID_WIDTH - XP_BAR_WIDTH) / 2;
            int barY = this.titleLabelY + 10;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_BAR_BACKGROUND, barX, barY, XP_BAR_WIDTH, XP_BAR_HEIGHT);
            int filled = Math.round(XP_BAR_WIDTH * ProgressionService.xpFraction(state, ProgressionService.maxLevel(villager)));
            if (filled > 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_BAR_CURRENT, XP_BAR_WIDTH, XP_BAR_HEIGHT, 0, 0, barX, barY, filled, XP_BAR_HEIGHT);
            }
            drawUnspentPointIndicator(graphics, state, barX, barY, XP_BAR_WIDTH, XP_BAR_HEIGHT);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = leftPos;
        int yo = topPos;

        // Back to front: the two groups sit behind the panel (2026-09-24 redesign), the meters on top of their tiles.
        boolean passiveStation = VillagerStateAccess.of(villager).getState().stations().passive().isPresent();
        if (passiveStation) {
            blitSprite(graphics, VillagerGuiTextures.PASSIVE_ABILITY_GROUP, xo + PASSIVE_GROUP_X, yo + PASSIVE_GROUP_Y);
        }
        List<Meter> meters = visibleMeters();
        int tilesX = STAT_GROUP_RIGHT_X - meters.size() * VillagerGuiTextures.STAT_GROUP_MIDDLE.width();
        blitSprite(graphics, VillagerGuiTextures.STAT_GROUP_LEFT, xo + tilesX - VillagerGuiTextures.STAT_GROUP_LEFT.width(), yo + STAT_GROUP_Y);
        for (int i = 0; i < meters.size(); i++) {
            blitSprite(graphics, VillagerGuiTextures.STAT_GROUP_MIDDLE, xo + tilesX + i * VillagerGuiTextures.STAT_GROUP_MIDDLE.width(), yo + STAT_GROUP_Y);
        }
        blitSprite(graphics, VillagerGuiTextures.STAT_GROUP_RIGHT, xo + STAT_GROUP_RIGHT_X, yo + STAT_GROUP_Y);
        blitSprite(graphics, VillagerGuiTextures.PANEL, xo + PANEL_X, yo);
        if (passiveStation) {
            graphics.text(this.font, "Work", xo + PASSIVE_GROUP_X + 5, yo + PASSIVE_GROUP_Y + 5, TITLE_COLOR, false);
        }

        drawBar(graphics, VillagerGuiTextures.HAPPINESS_BACKGROUND, VillagerGuiTextures.HAPPINESS_CURRENT,
                xo + HAPPINESS_BAR_X, xo + HAPPINESS_BAR_X + 2, yo + HAPPINESS_BAR_Y, happinessPercent() / 100.0F);
        for (Meter meter : meters) {
            drawBar(graphics, meter.background(), meter.fill(), xo + meter.x(), xo + meter.x() + METER_FILL_OFFSET, yo + METER_Y, meterFraction(meter.slot()));
        }

        // Section frames scroll with the list and are clipped to the list viewport.
        int ys = yo - scrollOffset;
        graphics.enableScissor(xo + LIST_X, yo + LIST_TOP, xo + LIST_X + LIST_WIDTH, yo + LIST_TOP + LIST_HEIGHT);
        for (SectionLayout section : sectionLayouts) {
            blitSprite(graphics, VillagerGuiTextures.TRADE_GROUP_TOP, xo + LIST_X, ys + section.headerY());
            for (int i = 0; i < Math.max(section.rowCount(), 1); i++) {
                blitSprite(graphics, VillagerGuiTextures.TRADE_GROUP_SIDES, xo + LIST_X, ys + section.contentY() + i * ROW_HEIGHT);
            }
            blitSprite(graphics, VillagerGuiTextures.TRADE_GROUP_BOTTOM, xo + LIST_X, ys + section.bottomY());
            graphics.text(this.font, section.title(), xo + LIST_X + 3, ys + section.headerY() + 3, TITLE_COLOR, false);
        }
        graphics.disableScissor();

        // MerchantScreen.extractScroller: the scroller slides along the track, disabled when everything fits.
        if (maxScroll > 0) {
            int scrollerY = yo + LIST_TOP + Math.round((LIST_HEIGHT - SCROLLER_HEIGHT) * (scrollOffset / (float) maxScroll));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, xo + SCROLLER_X, scrollerY, SCROLLER_WIDTH, SCROLLER_HEIGHT);
        } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_DISABLED_SPRITE, xo + SCROLLER_X, yo + LIST_TOP, SCROLLER_WIDTH, SCROLLER_HEIGHT);
        }

        // MerchantScreen.extractBackground: the big X when the selected offer is sold out.
        MerchantOffers offers = menu.getOffers();
        if (selectedOffer >= 0 && selectedOffer < offers.size() && offers.get(selectedOffer).isOutOfStock()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, OUT_OF_STOCK_SPRITE, xo + OUT_OF_STOCK_X, yo + OUT_OF_STOCK_Y, 28, 21);
        }
    }

    /** Row items are drawn after the widgets, on top of the row buttons - as in MerchantScreen.extractContents. */
    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        if (isHovering(HAPPINESS_BAR_X, HAPPINESS_BAR_Y, VillagerGuiTextures.HAPPINESS_BACKGROUND.width(),
                VillagerGuiTextures.HAPPINESS_BACKGROUND.height(), mouseX, mouseY)) {
            var happiness = VillagerStateAccess.of(villager).getState().happiness();
            Component tooltip = switch (happiness.mood()) {
                case 0 -> Component.translatable("gui.villageroverhaul.happiness.afraid", happiness.percent());
                case 100 -> Component.translatable("gui.villageroverhaul.happiness.hero", happiness.percent());
                default -> Component.translatable("gui.villageroverhaul.happiness", happiness.percent());
            };
            graphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
        for (Meter meter : visibleMeters()) {
            if (isHovering(meter.x(), METER_Y, meter.background().width(), meter.background().height(), mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(this.font, meterTooltip(meter.slot()), mouseX, mouseY);
            }
        }
        // List widgets and row items are clipped to the list viewport, like the section frames in
        // extractBackground, so they slide behind the edge instead of popping out.
        graphics.enableScissor(leftPos + LIST_X, topPos + LIST_TOP, leftPos + LIST_X + LIST_WIDTH, topPos + LIST_TOP + LIST_HEIGHT);
        for (AbstractWidget widget : listWidgets) {
            widget.extractRenderState(graphics, mouseX, mouseY, a);
        }
        extractRowItems(graphics, mouseX, mouseY);
        graphics.disableScissor();
    }

    private void extractRowItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MerchantOffers offers = menu.getOffers();
        for (TradeOfferButton button : tradeOfferButtons) {
            if (!button.visible || button.index >= offers.size()) {
                continue;
            }
            button.active = !isEmptyRow(button.index);
            if (!button.active) {
                continue; // a rank without a designed trade yet: the row stays an empty, disabled button
            }
            MerchantOffer offer = offers.get(button.index);
            int itemY = button.getY() + ROW_ITEM_Y;
            extractAndDecorateCost(graphics, offer.getCostA(), offer.getBaseCostA(), button.getX() + ROW_INPUT_X, itemY);
            ItemStack costB = offer.getCostB();
            if (!costB.isEmpty()) {
                graphics.fakeItem(costB, button.getX() + ROW_SECOND_INPUT_X, itemY);
                graphics.itemDecorations(this.font, costB, button.getX() + ROW_SECOND_INPUT_X, itemY);
            }
            Identifier arrow = offer.isOutOfStock() ? TRADE_ARROW_OUT_OF_STOCK_SPRITE : TRADE_ARROW_SPRITE;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrow, button.getX() + button.arrowX(), itemY + 3, 10, 9);
            ItemStack result = offer.getResult();
            graphics.fakeItem(result, button.getX() + button.resultX(), itemY);
            graphics.itemDecorations(this.font, result, button.getX() + button.resultX(), itemY);

            if (button.isHoveredOrFocused()) {
                button.extractToolTip(graphics, offer, mouseX, mouseY);
            }
        }
        rerollButtons.forEach((button, slot) -> {
            // isHovered, not isMouseOver: Vanilla's isMouseOver is false for an inactive button, and the
            // cooldown timer is exactly what the inactive button should show. Set while drawing, inside the list scissor.
            if (button.visible && button.isHovered()) {
                graphics.setTooltipForNextFrame(this.font, rerollTooltip(slot), mouseX, mouseY);
            }
        });
    }

    /**
     * MerchantScreen.extractAndDecorateCostA, including NeoForge's MCForge#8806 fix: on a discount
     * (Hero of the Village), the base count is struck through and the real count drawn beside it.
     */
    private void extractAndDecorateCost(GuiGraphicsExtractor graphics, ItemStack cost, ItemStack baseCost, int x, int y) {
        graphics.fakeItem(cost, x, y);
        if (baseCost.getCount() == cost.getCount()) {
            graphics.itemDecorations(this.font, cost, x, y);
        } else {
            graphics.itemDecorations(this.font, baseCost, x, y, baseCost.getCount() == 1 ? "1" : null);
            String count = cost.getCount() == 1 ? "1" : String.valueOf(cost.getCount());
            graphics.text(this.font, count, x + 14 + 19 - 2 - this.font.width(count), y + 6 + 3, 0xFFFFFFFF, true);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISCOUNT_STRIKETHROUGH_SPRITE, x + 7, y + 12, 9, 2);
        }
    }

    /**
     * Only next to the XP bar (2026-09-23: not next to every trade_level_button) - see the call in
     * extractLabels. The empty square always shows; the digit and star only appear once there's
     * actually an unspent point. Affordable groups additionally get a bobbing arrow at their rank
     * button (TradeLevelButtonWidget.setShowArrow).
     */
    private void drawUnspentPointIndicator(GuiGraphicsExtractor graphics, VillagerState state, int anchorX, int anchorY, int anchorWidth, int anchorHeight) {
        int squareX = anchorX + anchorWidth + 7;
        int squareY = anchorY + anchorHeight / 2 - VillagerGuiTextures.SKILL_POINT_SQUARE.height() / 2;
        blitSprite(graphics, VillagerGuiTextures.SKILL_POINT_SQUARE, squareX, squareY);

        int unspent = state.unspentUpgradePoints();
        if (unspent <= 0) {
            return;
        }

        int digitIndex = Math.min(unspent, VillagerGuiTextures.SKILL_POINT_DIGITS.length) - 1;
        VillagerGuiTextures.Sprite digit = VillagerGuiTextures.SKILL_POINT_DIGITS[digitIndex];
        int digitX = squareX + (VillagerGuiTextures.SKILL_POINT_SQUARE.width() - digit.width()) / 2;
        int digitY = squareY + (VillagerGuiTextures.SKILL_POINT_SQUARE.height() - digit.height()) / 2;
        blitSprite(graphics, digit, digitX, digitY);

        // Measured square-relative in the pxo (skill_point_star at (356,75) vs skill_point_square at (348,66)).
        blitSprite(graphics, VillagerGuiTextures.SKILL_POINT_STAR, squareX + 8, squareY + 9);
    }

    /** The server-computed happiness right now (momentary mood included), synced with the state - see Happiness.effectivePercent. */
    private int happinessPercent() {
        return VillagerStateAccess.of(villager).getState().happiness().effectivePercent();
    }

    /** A productivity meter in the stat group; x is screen-relative (its background's left edge). */
    private record Meter(StationSlot slot, VillagerGuiTextures.Sprite background, VillagerGuiTextures.Sprite fill, int x) {
    }

    /**
     * The meters of the workplaces the villager owns (2026-09-24), left to right passive, master, basic,
     * packed against the happiness piece - a missing one lets the ones left of it slide right.
     */
    private List<Meter> visibleMeters() {
        var stations = VillagerStateAccess.of(villager).getState().stations();
        List<StationSlot> order = List.of(StationSlot.PASSIVE, StationSlot.MASTER, StationSlot.BASIC);
        List<StationSlot> shown = order.stream().filter(slot -> stations.get(slot).isPresent()).toList();
        int tilesX = STAT_GROUP_RIGHT_X - shown.size() * VillagerGuiTextures.STAT_GROUP_MIDDLE.width();
        List<Meter> meters = new ArrayList<>(shown.size());
        for (int i = 0; i < shown.size(); i++) {
            StationSlot slot = shown.get(i);
            int x = tilesX + i * VillagerGuiTextures.STAT_GROUP_MIDDLE.width() + METER_TILE_OFFSET;
            meters.add(switch (slot) {
                case PASSIVE -> new Meter(slot, VillagerGuiTextures.PASSIVE_PRODUCTIVITY_BACKGROUND, VillagerGuiTextures.PASSIVE_PRODUCTIVITY_CURRENT, x);
                case MASTER -> new Meter(slot, VillagerGuiTextures.MASTER_PRODUCTIVITY_BACKGROUND, VillagerGuiTextures.MASTER_PRODUCTIVITY_CURRENT, x);
                case BASIC -> new Meter(slot, VillagerGuiTextures.BASIC_PRODUCTIVITY_BACKGROUND, VillagerGuiTextures.BASIC_PRODUCTIVITY_CURRENT, x);
            });
        }
        return meters;
    }

    /** How full a workplace's productivity meter is; full means its trades refill a step - see RestockService. */
    private float meterFraction(StationSlot slot) {
        VillagerState state = VillagerStateAccess.of(villager).getState();
        int points = state.dailyProductivity().of(slot);
        return Math.min(1.0F, points / (float) RestockService.meterPoints(villager, slot, state));
    }

    /** Passive has no trades yet - its tooltip is just how full the meter is. */
    private Component meterTooltip(StationSlot slot) {
        return slot == StationSlot.PASSIVE
                ? Component.translatable("gui.villageroverhaul.meter.passive", Math.round(meterFraction(slot) * 100))
                : meterTooltip(slot.tier());
    }

    /**
     * One line: the category's emptiest trade, as stock relative to its max ("Trade Restock: 40%") -
     * read from the menu's offers, whose order is quests, basic trades, master trades.
     */
    private Component meterTooltip(ItemExchange.Tier tier) {
        MerchantOffers offers = menu.getOffers();
        int questCount = Math.min(menu.questCount(), offers.size());
        int basicCount = Math.min(menu.basicCount(), offers.size() - questCount);
        int from = tier == ItemExchange.Tier.MASTER ? questCount + basicCount : questCount;
        int to = tier == ItemExchange.Tier.MASTER ? offers.size() : questCount + basicCount;
        int lowestPercent = -1;
        for (int i = from; i < to; i++) {
            MerchantOffer offer = offers.get(i);
            if (offer.getMaxUses() > 0) {
                int percent = 100 * (offer.getMaxUses() - offer.getUses()) / offer.getMaxUses();
                lowestPercent = lowestPercent < 0 ? percent : Math.min(lowestPercent, percent);
            }
        }
        String key = "gui.villageroverhaul.meter." + (tier == ItemExchange.Tier.MASTER ? "master" : "basic");
        return lowestPercent < 0
                ? Component.translatable(key + ".none")
                : Component.translatable(key, lowestPercent);
    }

    private void drawBar(GuiGraphicsExtractor graphics, VillagerGuiTextures.Sprite background, VillagerGuiTextures.Sprite fill, int bgX, int fillX, int topY, float fraction) {
        blitSprite(graphics, background, bgX, topY);
        int fillHeight = Math.round(fill.height() * fraction);
        if (fillHeight <= 0) {
            return;
        }
        int srcY = fill.height() - fillHeight;
        graphics.blit(RenderPipelines.GUI_TEXTURED, fill.texture(), fillX, topY + srcY, 0.0F, srcY,
                fill.width(), fillHeight, fill.width(), fillHeight, fill.width(), fill.height());
    }

    private void blitSprite(GuiGraphicsExtractor graphics, VillagerGuiTextures.Sprite sprite, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, sprite.texture(), x, y, 0.0F, 0.0F, sprite.width(), sprite.height(), sprite.width(), sprite.height());
    }

    /** MerchantScreen.TradeOfferButton - a plain Vanilla button per row, bound to one offer index. */
    private class TradeOfferButton extends Button.Plain {
        final int index;

        TradeOfferButton(int x, int y, int index) {
            super(x, y, ROW_BUTTON_WIDTH, ROW_BUTTON_HEIGHT, CommonComponents.EMPTY, button -> {
                if (!isEmptyRow(index)) {
                    selectOffer(index);
                }
            }, DEFAULT_NARRATION);
            this.index = index;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return super.isMouseOver(mouseX, mouseY) && isInListViewport(mouseX, mouseY);
        }

        private boolean isQuestRow() {
            return index < menu.questCount();
        }

        int arrowX() {
            return isQuestRow() ? QUEST_ROW_ARROW_X : ROW_ARROW_X;
        }

        int resultX() {
            return isQuestRow() ? QUEST_ROW_RESULT_X : ROW_RESULT_X;
        }

        /**
         * Which item icon a horizontal position falls on: costA (+discount count), costB, result - or null
         * (arrow, empty costB, a quest row's reroll button).
         */
        VillagerMenu.OfferItem itemAt(MerchantOffer offer, double mouseX) {
            if (mouseX < this.getX() + Math.min(ROW_SECOND_INPUT_X, arrowX()) - 2) {
                return VillagerMenu.OfferItem.COST_A;
            } else if (mouseX < this.getX() + arrowX()) {
                return offer.getCostB().isEmpty() ? null : VillagerMenu.OfferItem.COST_B;
            } else if (mouseX >= this.getX() + resultX() - 2 && mouseX < this.getX() + resultX() + 16) {
                return VillagerMenu.OfferItem.RESULT;
            }
            return null;
        }

        void extractToolTip(GuiGraphicsExtractor graphics, MerchantOffer offer, int mouseX, int mouseY) {
            if (!this.isHovered) {
                return;
            }
            VillagerMenu.OfferItem item = itemAt(offer, mouseX);
            if (item != null) {
                graphics.setTooltipForNextFrame(VillagerScreen.this.font, item.of(offer), mouseX, mouseY);
            }
        }
    }
}
