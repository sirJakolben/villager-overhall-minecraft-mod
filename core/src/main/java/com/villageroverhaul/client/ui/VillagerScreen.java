package com.villageroverhaul.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.menu.VillagerMenu;
import com.villageroverhaul.network.CloneOfferItemPayload;
import com.villageroverhaul.network.InvestUpgradePointPayload;
import com.villageroverhaul.network.SectionActionPayload;
import com.villageroverhaul.network.SelectOfferPayload;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.section.SectionView;
import com.villageroverhaul.section.Sections;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.trade.MissingTrade;
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
import java.util.List;
import java.util.Optional;

/**
 * Our layout (villager_gui.pxo, 2026-09-22 brainstorm) around Vanilla's trading behavior. Every row
 * reads straight from the menu's MerchantOffers, the same list MerchantScreen reads - so discount
 * strikethrough, out-of-stock arrow/X and item tooltips follow MerchantScreen's own drawing code
 * (extractAndDecorateCostA, extractButtonArrows, TradeOfferButton.extractToolTip). Clicking a row
 * runs setSelectionHint + tryMoveItems locally for prediction and sends SelectOfferPayload, like
 * MerchantScreen.postButtonClick.
 *
 * Built from the open sections the server sent (menu.sections(), in section order): every LIST section is a
 * group in the scrolling list with its title, rank button and rows (its rows' look: SectionClientLogic); the
 * BADGE section is the badge at the top left. Every section with a shown meter gets one in the stat group, the
 * first section nearest the happiness meter. Rows are rebuilt whenever the offers or the synced VillagerState
 * change. The list scrolls (wheel or scroller drag) once it outgrows the panel; per-row stock display is
 * intentionally absent for now (2026-09-23).
 */
public class VillagerScreen extends AbstractContainerScreen<VillagerMenu> {

    private static final int LIST_X = 48;
    private static final int LIST_WIDTH = 88;
    private static final int SECTION_START_Y = 8;
    // Rows are 20px apart, the button height, so buttons touch. Each 21px section side tile starts with a 1px
    // dark line; tiles are drawn 20px apart, so only the first line (the frame's top edge) shows - the others
    // sit under the button above. Buttons start below that line.
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
    // badge and the stat group sit behind it and stick out to the left.
    private static final int SCREEN_WIDTH = 319;
    private static final int PANEL_X = 41;
    private static final int BADGE_X = -7;
    private static final int BADGE_Y = 5;
    private static final int BADGE_BUTTON_X = 23;
    private static final int BADGE_BUTTON_Y = 8;
    // Stat group: left cap, one middle tile per visible meter, right piece (holds happiness) at a fixed spot.
    private static final int STAT_GROUP_RIGHT_X = 27;
    private static final int STAT_GROUP_Y = 71;
    // Happiness meter background (9x78) - also its tooltip hover area.
    private static final int HAPPINESS_BAR_X = 29;
    private static final int HAPPINESS_BAR_Y = 77;
    // Work meter backgrounds (11x79) sit 1px into their middle tile, the fill 3px into the background.
    private static final int METER_TILE_OFFSET = 1;
    private static final int METER_FILL_OFFSET = 3;
    private static final int METER_Y = 77;
    private static final int TITLE_COLOR = 0xFF404040;

    // Rows sit inside the section frame, 3px in from each side - Vanilla's own 20px button height.
    private static final int ROW_BUTTON_INSET = 3;
    private static final int ROW_BUTTON_WIDTH = LIST_WIDTH - 2 * ROW_BUTTON_INSET;
    private static final int ROW_BUTTON_HEIGHT = 20;
    // Row-relative positions of costA and costB. The discounted count is drawn right of the costA icon (up to
    // +31, see extractAndDecorateCost), which is why costB starts that far right - as in Vanilla, the count may
    // touch costB's edge. Arrow and result: SectionClientLogic.
    private static final int ROW_INPUT_X = 2;
    private static final int ROW_SECOND_INPUT_X = 32;
    private static final int ROW_ITEM_Y = 2;

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

    /** A LIST section in the scrolling list; firstOffer is the index of its first row in the menu's offer list. */
    private record SectionLayout(SectionDefinition section, SectionView view, int headerY, int contentY, int bottomY, int firstOffer) {
    }

    /** A work meter in the stat group; x is screen-relative (its background's left edge). */
    private record Meter(SectionDefinition section, SectionView view, VillagerGuiTextures.MeterSprites sprites, int x) {
    }

    private final Villager villager;
    private final List<OfferRowButton> rowButtons = new ArrayList<>();
    private final List<SectionClientLogic.RowWidget> rowWidgets = new ArrayList<>();
    /** Row buttons, row widgets and section rank buttons: input via addWidget, drawn by hand under the list scissor. */
    private final List<AbstractWidget> listWidgets = new ArrayList<>();
    private List<SectionLayout> sectionLayouts = List.of();
    private List<Meter> meters = List.of();
    private Optional<SectionDefinition> badge = Optional.empty();
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
     * Only what the buttons show (rank labels, upgrade arrows) - XP, happiness and meters change every few
     * seconds while the villager works, but are drawn fresh each frame and need no rebuild.
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

        List<SectionLayout> layouts = new ArrayList<>();
        List<Meter> shownMeters = new ArrayList<>();
        Optional<SectionDefinition> badgeSection = Optional.empty();
        int y = SECTION_START_Y;
        int firstOffer = 0;
        for (SectionView view : menu.sections()) {
            Optional<SectionDefinition> definition = Sections.get(view.id());
            if (definition.isEmpty()) {
                continue;
            }
            SectionDefinition section = definition.get();
            if (section.display() == SectionDefinition.Display.BADGE) {
                badgeSection = definition;
                addRankButton(section, view, leftPos + BADGE_BUTTON_X, topPos + BADGE_BUTTON_Y, state, false);
            } else {
                int contentY = y + VillagerGuiTextures.SECTION_TOP.height();
                int bottomY = contentY + ROW_TOP_BORDER + Math.max(view.rows(), 1) * ROW_HEIGHT;
                SectionLayout layout = new SectionLayout(section, view, y, contentY, bottomY, firstOffer);
                layouts.add(layout);
                addSectionRankButton(layout, state);
                y = bottomY + VillagerGuiTextures.SECTION_BOTTOM.height();
                firstOffer += view.rows();
            }
            if (view.showsMeter()) {
                shownMeters.add(new Meter(section, view, VillagerGuiTextures.MeterSprites.of(section.meterSprite()), 0));
            }
        }
        this.sectionLayouts = layouts;
        this.badge = badgeSection;
        this.meters = placeMeters(shownMeters);
        this.maxScroll = Math.max(0, y - LIST_TOP - LIST_HEIGHT);
        this.scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
        rebuildRows(layouts);
    }

    /** Right to left from the happiness meter, first section first - so the section order reads right to left. */
    private static List<Meter> placeMeters(List<Meter> shown) {
        int middle = VillagerGuiTextures.STAT_GROUP_MIDDLE.width();
        int tilesX = STAT_GROUP_RIGHT_X - shown.size() * middle;
        List<Meter> placed = new ArrayList<>(shown.size());
        for (int i = 0; i < shown.size(); i++) {
            Meter meter = shown.get(i);
            int tile = shown.size() - 1 - i;
            placed.add(new Meter(meter.section(), meter.view(), meter.sprites(), tilesX + tile * middle + METER_TILE_OFFSET));
        }
        return placed;
    }

    private void rebuildRows(List<SectionLayout> layouts) {
        rowButtons.clear();
        rowWidgets.clear();
        for (SectionLayout layout : layouts) {
            SectionClientLogic clientLogic = SectionClientLogic.of(layout.section().id());
            RowContext context = new RowContext(layout.section());
            for (int i = 0; i < layout.view().rows(); i++) {
                int x = leftPos + LIST_X + ROW_BUTTON_INSET;
                int rowY = layout.contentY() + ROW_TOP_BORDER + i * ROW_HEIGHT - scrollOffset;
                int index = layout.firstOffer() + i;
                boolean visible = touchesList(rowY, ROW_BUTTON_HEIGHT);
                int slot = index < menu.rowSlots().size() ? menu.rowSlots().get(index) : -1;
                // Added before its row button, so a click on it doesn't select the row; drawn after it.
                SectionClientLogic.RowWidget rowWidget = clientLogic.rowWidget(context, slot, x, topPos + rowY);
                if (rowWidget != null) {
                    rowWidget.widget().visible = visible;
                    addWidget(rowWidget.widget());
                    rowWidgets.add(rowWidget);
                }
                OfferRowButton button = addWidget(new OfferRowButton(x, topPos + rowY, index, clientLogic));
                button.visible = visible;
                button.active = !isEmptyRow(index);
                rowButtons.add(button);
                listWidgets.add(button);
                if (rowWidget != null) {
                    listWidgets.add(rowWidget.widget());
                }
            }
        }
    }

    /** What a section's row widgets may use of this screen (SectionClientLogic.RowContext). */
    private final class RowContext implements SectionClientLogic.RowContext {
        private final SectionDefinition section;

        private RowContext(SectionDefinition section) {
            this.section = section;
        }

        @Override
        public Villager villager() {
            return villager;
        }

        @Override
        public SectionDefinition section() {
            return section;
        }

        @Override
        public boolean isInList(double mouseX, double mouseY) {
            return isInListViewport(mouseX, mouseY);
        }

        @Override
        public void sendAction(int slot, int action) {
            ClientPacketDistributor.sendToServer(new SectionActionPayload(section.id(), slot, action));
        }
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
        for (OfferRowButton button : rowButtons) {
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

    private void addSectionRankButton(SectionLayout layout, VillagerState state) {
        int x = leftPos + LIST_X + LIST_WIDTH - VillagerGuiTextures.RANK_BUTTON.width() - 2 + HEADER_BUTTON_X_ADJUST;
        int panelY = layout.headerY() - scrollOffset + (VillagerGuiTextures.SECTION_TOP.height() - VillagerGuiTextures.RANK_BUTTON.height()) / 2 + BUTTON_Y_ADJUST;
        RankButtonWidget button = addRankButton(layout.section(), layout.view(), x, topPos + panelY, state, true);
        button.visible = touchesList(panelY, VillagerGuiTextures.RANK_BUTTON.height());
        listWidgets.add(button);
    }

    /** inList: the button scrolls with the list (a section header) and is clipped there; otherwise it is a normal widget (the badge). */
    private RankButtonWidget addRankButton(SectionDefinition section, SectionView view, int x, int y, VillagerState state, boolean inList) {
        Runnable invest = () -> ClientPacketDistributor.sendToServer(new InvestUpgradePointPayload(section.id()));
        RankButtonWidget button = inList
                ? new RankButtonWidget(x, y, state.rank(section.id()), invest) {
                    @Override
                    public boolean isMouseOver(double mouseX, double mouseY) {
                        return super.isMouseOver(mouseX, mouseY) && isInListViewport(mouseX, mouseY);
                    }
                }
                : new RankButtonWidget(x, y, state.rank(section.id()), invest);
        int cost = view.nextUpgradeCost();
        button.setTooltip(Tooltip.create(cost < 0
                ? Component.translatable("gui.villageroverhaul.upgrade.maxed")
                : Component.translatable("gui.villageroverhaul.upgrade.cost", cost, state.unspentUpgradePoints())));
        button.setShowArrow(cost >= 0 && cost <= state.unspentUpgradePoints());
        return inList ? addWidget(button) : addRenderableWidget(button);
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

        // Back to front: badge and stat group sit behind the panel (2026-09-24 redesign), the meters on top of their tiles.
        if (badge.isPresent()) {
            blitSprite(graphics, VillagerGuiTextures.BADGE, xo + BADGE_X, yo + BADGE_Y);
        }
        int tilesX = STAT_GROUP_RIGHT_X - meters.size() * VillagerGuiTextures.STAT_GROUP_MIDDLE.width();
        blitSprite(graphics, VillagerGuiTextures.STAT_GROUP_LEFT, xo + tilesX - VillagerGuiTextures.STAT_GROUP_LEFT.width(), yo + STAT_GROUP_Y);
        for (int i = 0; i < meters.size(); i++) {
            blitSprite(graphics, VillagerGuiTextures.STAT_GROUP_MIDDLE, xo + tilesX + i * VillagerGuiTextures.STAT_GROUP_MIDDLE.width(), yo + STAT_GROUP_Y);
        }
        blitSprite(graphics, VillagerGuiTextures.STAT_GROUP_RIGHT, xo + STAT_GROUP_RIGHT_X, yo + STAT_GROUP_Y);
        blitSprite(graphics, VillagerGuiTextures.PANEL, xo + PANEL_X, yo);
        badge.ifPresent(section -> graphics.text(this.font, Component.translatable(section.titleKey()),
                xo + BADGE_X + 5, yo + BADGE_Y + 5, TITLE_COLOR, false));

        drawBar(graphics, VillagerGuiTextures.HAPPINESS_BACKGROUND, VillagerGuiTextures.HAPPINESS_CURRENT,
                xo + HAPPINESS_BAR_X, xo + HAPPINESS_BAR_X + 2, yo + HAPPINESS_BAR_Y, happinessPercent() / 100.0F);
        for (Meter meter : meters) {
            drawBar(graphics, meter.sprites().background(), meter.sprites().fill(), xo + meter.x(), xo + meter.x() + METER_FILL_OFFSET, yo + METER_Y, meterFraction(meter));
        }

        // Section frames scroll with the list and are clipped to the list viewport.
        int ys = yo - scrollOffset;
        graphics.enableScissor(xo + LIST_X, yo + LIST_TOP, xo + LIST_X + LIST_WIDTH, yo + LIST_TOP + LIST_HEIGHT);
        for (SectionLayout layout : sectionLayouts) {
            blitSprite(graphics, VillagerGuiTextures.SECTION_TOP, xo + LIST_X, ys + layout.headerY());
            for (int i = 0; i < Math.max(layout.view().rows(), 1); i++) {
                blitSprite(graphics, VillagerGuiTextures.SECTION_SIDES, xo + LIST_X, ys + layout.contentY() + i * ROW_HEIGHT);
            }
            blitSprite(graphics, VillagerGuiTextures.SECTION_BOTTOM, xo + LIST_X, ys + layout.bottomY());
            graphics.text(this.font, Component.translatable(layout.section().titleKey()), xo + LIST_X + 3, ys + layout.headerY() + 3, TITLE_COLOR, false);
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
        for (Meter meter : meters) {
            if (isHovering(meter.x(), METER_Y, meter.sprites().background().width(), meter.sprites().background().height(), mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(this.font, meterTooltip(meter), mouseX, mouseY);
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
        for (OfferRowButton button : rowButtons) {
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
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, arrow, button.getX() + button.layout.arrowX(), itemY + 3, 10, 9);
            ItemStack result = offer.getResult();
            graphics.fakeItem(result, button.getX() + button.layout.resultX(), itemY);
            graphics.itemDecorations(this.font, result, button.getX() + button.layout.resultX(), itemY);

            if (button.isHoveredOrFocused()) {
                button.extractToolTip(graphics, offer, mouseX, mouseY);
            }
        }
        for (SectionClientLogic.RowWidget rowWidget : rowWidgets) {
            // isHovered, not isMouseOver: Vanilla's isMouseOver is false for an inactive button, and an inactive
            // widget (a reroll on cooldown) is exactly when its tooltip matters. Set while drawing, inside the list scissor.
            if (rowWidget.widget().visible && rowWidget.widget().isHovered()) {
                graphics.setTooltipForNextFrame(this.font, rowWidget.tooltip().get(), mouseX, mouseY);
            }
        }
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
     * Only next to the XP bar (2026-09-23: not next to every rank button) - see the call in extractLabels.
     * The empty square always shows; the digit and star only appear once there's actually an unspent point.
     * Affordable sections additionally get a bobbing arrow at their rank button (RankButtonWidget.setShowArrow).
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

    /** How full a section's meter is (synced state, size from the server). */
    private float meterFraction(Meter meter) {
        int points = VillagerStateAccess.of(villager).getState().productivity().meter(meter.section().id());
        return Math.min(1.0F, points / (float) meter.view().meterPoints());
    }

    /**
     * A LIST section: its emptiest row, as stock relative to its max ("Trades restock: 40%"). A BADGE section has
     * no rows - just how full its meter is.
     */
    private Component meterTooltip(Meter meter) {
        Component title = Component.translatable(meter.section().titleKey());
        if (meter.section().display() == SectionDefinition.Display.BADGE) {
            return Component.translatable("gui.villageroverhaul.meter.progress", title, Math.round(meterFraction(meter) * 100));
        }
        MerchantOffers offers = menu.getOffers();
        int lowestPercent = -1;
        for (SectionLayout layout : sectionLayouts) {
            if (layout.section() != meter.section()) {
                continue;
            }
            for (int i = layout.firstOffer(); i < layout.firstOffer() + layout.view().rows() && i < offers.size(); i++) {
                MerchantOffer offer = offers.get(i);
                if (offer.getMaxUses() > 0) {
                    int percent = 100 * (offer.getMaxUses() - offer.getUses()) / offer.getMaxUses();
                    lowestPercent = lowestPercent < 0 ? percent : Math.min(lowestPercent, percent);
                }
            }
        }
        return lowestPercent < 0
                ? Component.translatable("gui.villageroverhaul.meter.restock.none", title)
                : Component.translatable("gui.villageroverhaul.meter.restock", title, lowestPercent);
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
    private class OfferRowButton extends Button.Plain {
        final int index;
        final SectionClientLogic layout;

        OfferRowButton(int x, int y, int index, SectionClientLogic layout) {
            super(x, y, ROW_BUTTON_WIDTH, ROW_BUTTON_HEIGHT, CommonComponents.EMPTY, button -> {
                if (!isEmptyRow(index)) {
                    selectOffer(index);
                }
            }, DEFAULT_NARRATION);
            this.index = index;
            this.layout = layout;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return super.isMouseOver(mouseX, mouseY) && isInListViewport(mouseX, mouseY);
        }

        /**
         * Which item icon a horizontal position falls on: costA (+discount count), costB, result - or null
         * (arrow, empty costB, a row widget).
         */
        VillagerMenu.OfferItem itemAt(MerchantOffer offer, double mouseX) {
            if (mouseX < this.getX() + Math.min(ROW_SECOND_INPUT_X, layout.arrowX()) - 2) {
                return VillagerMenu.OfferItem.COST_A;
            } else if (mouseX < this.getX() + layout.arrowX()) {
                return offer.getCostB().isEmpty() ? null : VillagerMenu.OfferItem.COST_B;
            } else if (mouseX >= this.getX() + layout.resultX() - 2 && mouseX < this.getX() + layout.resultX() + 16) {
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
