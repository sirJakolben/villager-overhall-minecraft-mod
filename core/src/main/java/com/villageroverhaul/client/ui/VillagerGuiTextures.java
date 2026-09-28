package com.villageroverhaul.client.ui;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.resources.Identifier;

/**
 * The villager screen's hand-drawn pieces, extracted from assets/villager_gui/villager_gui.pxo
 * (last on 2026-09-24) - see extracted/layer_bounds_2026-09-24.json in that folder for the original
 * pxo-canvas coordinates this sizing came from. The file names are the pxo layer names. Row arrows, discount
 * strikethrough and out-of-stock marks are not here - VillagerScreen uses Vanilla's own
 * container/villager/* sprites for those directly.
 */
public final class VillagerGuiTextures {

    public record Sprite(Identifier texture, int width, int height) {
        /** Plain blit() in 26.1 wants the full resource path, unlike blitSprite()'s atlas lookup (see Vanilla's FurnaceScreen). */
        public static Sprite of(Identifier name, int width, int height) {
            return new Sprite(Identifier.fromNamespaceAndPath(name.getNamespace(), "textures/gui/villager/" + name.getPath() + ".png"), width, height);
        }

        private static Sprite core(String name, int width, int height) {
            return of(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, name), width, height);
        }
    }

    /** A section's work meter: its background and the fill drawn into it (SectionDefinition.meterSprite). */
    public record MeterSprites(Sprite background, Sprite fill) {
        public static MeterSprites of(Identifier meterSprite) {
            return new MeterSprites(
                    Sprite.of(meterSprite.withSuffix("_background"), 11, 79),
                    Sprite.of(meterSprite.withSuffix("_current"), 5, 66));
        }
    }

    public static final Sprite PANEL = Sprite.core("villager_gui", 278, 166);
    /** Backdrop of the BADGE section at the top left - a neutral frame (pxo layer "passive_ability_group"). */
    public static final Sprite BADGE = Sprite.core("badge_frame", 56, 67);
    // The meter backdrop behind the panel: left cap, one middle tile per visible meter, right piece holding happiness.
    public static final Sprite STAT_GROUP_LEFT = Sprite.core("stat_group_left", 4, 90);
    public static final Sprite STAT_GROUP_MIDDLE = Sprite.core("stat_group_middle", 13, 90);
    public static final Sprite STAT_GROUP_RIGHT = Sprite.core("stat_group_right", 18, 90);

    public static final Sprite HAPPINESS_BACKGROUND = Sprite.core("happiness_background", 9, 78);
    public static final Sprite HAPPINESS_CURRENT = Sprite.core("happiness_current", 5, 66);

    /** A LIST section's frame in the scrolling list: header, one side tile per row, bottom. */
    public static final Sprite SECTION_TOP = Sprite.core("trade_group_top", 88, 14);
    public static final Sprite SECTION_SIDES = Sprite.core("trade_group_sides", 88, 21);
    public static final Sprite SECTION_BOTTOM = Sprite.core("trade_group_bottom", 88, 5);

    public static final Sprite RANK_BUTTON = Sprite.core("trade_level_button", 18, 13);
    public static final Sprite RANK_ARROW = Sprite.core("trade_level_arrow", 8, 10);

    public static final Sprite SKILL_POINT_SQUARE = Sprite.core("skill_point_square", 15, 15);
    public static final Sprite SKILL_POINT_STAR = Sprite.core("skill_point_star", 13, 13);
    public static final Sprite[] SKILL_POINT_DIGITS = {
            Sprite.core("skill_point_nbr1", 7, 11),
            Sprite.core("skill_point_nbr2", 9, 11),
            Sprite.core("skill_point_nbr3", 8, 10),
            Sprite.core("skill_point_nbr4", 9, 11),
            Sprite.core("skill_point_nbr5", 7, 11)
    };

    private VillagerGuiTextures() {
    }
}
