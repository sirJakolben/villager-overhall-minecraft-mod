package com.villageroverhaul.client.ui;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.resources.Identifier;

/**
 * The villager screen's hand-drawn pieces, extracted from assets/villager_gui/villager_gui.pxo
 * (last on 2026-09-24) - see extracted/layer_bounds_2026-09-24.json in that folder for the original
 * pxo-canvas coordinates this sizing came from. Row arrows, discount
 * strikethrough and out-of-stock marks are not here - VillagerScreen uses Vanilla's own
 * container/villager/* sprites for those directly.
 */
public final class VillagerGuiTextures {

    public record Sprite(Identifier texture, int width, int height) {
        private Sprite(String name, int width, int height) {
            // Plain blit() in 26.1 wants the full resource path, unlike blitSprite()'s atlas lookup -
            // confirmed against Vanilla's own FurnaceScreen.TEXTURE ("textures/gui/container/furnace.png").
            this(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "textures/gui/villager/" + name + ".png"), width, height);
        }
    }

    public static final Sprite PANEL = new Sprite("villager_gui", 278, 166);
    public static final Sprite PASSIVE_ABILITY_GROUP = new Sprite("passive_ability_group", 56, 67);
    // The meter backdrop behind the panel: left cap, one middle tile per visible meter, right piece holding happiness.
    public static final Sprite STAT_GROUP_LEFT = new Sprite("stat_group_left", 4, 90);
    public static final Sprite STAT_GROUP_MIDDLE = new Sprite("stat_group_middle", 13, 90);
    public static final Sprite STAT_GROUP_RIGHT = new Sprite("stat_group_right", 18, 90);

    public static final Sprite HAPPINESS_BACKGROUND = new Sprite("happiness_background", 9, 78);
    public static final Sprite HAPPINESS_CURRENT = new Sprite("happiness_current", 5, 66);
    public static final Sprite BASIC_PRODUCTIVITY_BACKGROUND = new Sprite("basic_productivity_background", 11, 79);
    public static final Sprite BASIC_PRODUCTIVITY_CURRENT = new Sprite("basic_productivity_current", 5, 66);
    public static final Sprite MASTER_PRODUCTIVITY_BACKGROUND = new Sprite("master_productivity_background", 11, 79);
    public static final Sprite MASTER_PRODUCTIVITY_CURRENT = new Sprite("master_productivity_current", 5, 66);
    public static final Sprite PASSIVE_PRODUCTIVITY_BACKGROUND = new Sprite("passive_productivity_background", 11, 79);
    public static final Sprite PASSIVE_PRODUCTIVITY_CURRENT = new Sprite("passive_productivity_current", 5, 66);

    public static final Sprite TRADE_GROUP_TOP = new Sprite("trade_group_top", 88, 14);
    public static final Sprite TRADE_GROUP_SIDES = new Sprite("trade_group_sides", 88, 21);
    public static final Sprite TRADE_GROUP_BOTTOM = new Sprite("trade_group_bottom", 88, 5);

    public static final Sprite TRADE_LEVEL_BUTTON = new Sprite("trade_level_button", 18, 13);
    public static final Sprite TRADE_LEVEL_ARROW = new Sprite("trade_level_arrow", 8, 10);

    // Quest row reroll button (2026-09-26), cropped from villager_gui/reroll_button_active/_inactive.png (canvas x 178, y 77).
    public static final Sprite REROLL_BUTTON = new Sprite("reroll_button", 15, 15);
    public static final Sprite REROLL_BUTTON_DISABLED = new Sprite("reroll_button_disabled", 15, 15);

    public static final Sprite SKILL_POINT_SQUARE = new Sprite("skill_point_square", 15, 15);
    public static final Sprite SKILL_POINT_STAR = new Sprite("skill_point_star", 13, 13);
    public static final Sprite[] SKILL_POINT_DIGITS = {
            new Sprite("skill_point_nbr1", 7, 11),
            new Sprite("skill_point_nbr2", 9, 11),
            new Sprite("skill_point_nbr3", 8, 10),
            new Sprite("skill_point_nbr4", 9, 11),
            new Sprite("skill_point_nbr5", 7, 11)
    };

    private VillagerGuiTextures() {
    }
}
