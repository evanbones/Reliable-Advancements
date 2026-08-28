package com.evandev.reliable_advancements.gui.theme;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class EditorTheme {
    public static final int BG_OVERLAY = 0xCC0A0C10;
    public static final int PANEL_BG = 0xF414161F;
    public static final int SIDEBAR_BG = 0xF80E1017;
    public static final int CARD_BG = 0x881B1E2B;
    public static final int CARD_BORDER = 0x443D445F;

    public static final int BORDER_OUTER = 0xFF08090C;
    public static final int BORDER_INNER = 0xFF2A2E3F;
    public static final int ACCENT_GOLD = 0xFFE5C158;
    public static final int ACCENT_GOLD_MUTED = 0xFFA88C48;
    public static final int ACCENT_BLUE = 0xFF4C6EF5;

    public static final int TAB_INACTIVE_BG = 0x66181B26;
    public static final int TAB_HOVER_BG = 0xCC232738;
    public static final int TAB_ACTIVE_BG = 0xFF2B3045;
    public static final int TAB_ACTIVE_BAR = 0xFFE5C158;

    public static final int TEXT_TITLE = 0xFFFFFFFF;
    public static final int TEXT_PRIMARY = 0xFFECEEF4;
    public static final int TEXT_LABEL = 0xFFBAC0D6;
    public static final int TEXT_MUTED = 0xFF767C96;
    public static final int TEXT_GOLD = 0xFFE5C158;
    public static final int TEXT_GREEN = 0xFF51CF66;
    public static final int TEXT_RED = 0xFFFF6B6B;

    public static final int BORDER_VALID = 0xFF40C057;
    public static final int BORDER_ERROR = 0xFFFA5252;
    public static final int BORDER_DEFAULT = 0xFF3B4056;
    public static final int BORDER_FOCUSED = 0xFF748FFC;

    public static final int SCROLL_TRACK = 0x880A0B10;
    public static final int SCROLL_THUMB = 0xFF3D445C;
    public static final int SCROLL_THUMB_HOVER = 0xFF5C668A;

    public static final Identifier ICON_CHEVRON_DOWN = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/icons/chevron_down.png");
    public static final Identifier ICON_CHEVRON_DOWN_HOVER = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/icons/chevron_down_hover.png");
    public static final Identifier ICON_CHEVRON_DOWN_GOLD = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/icons/chevron_down_gold.png");
    public static final Identifier ICON_CHEVRON_RIGHT = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/icons/chevron_right.png");
    public static final Identifier ICON_CHEVRON_RIGHT_HOVER = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/icons/chevron_right_hover.png");
    public static final Identifier ICON_CHEVRON_RIGHT_GOLD = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/icons/chevron_right_gold.png");

    private EditorTheme() {
    }

    public static void drawWindow(GuiGraphicsExtractor gfx, int x, int y, int w, int h) {
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, BORDER_OUTER);
        gfx.fill(x, y, x + w, y + h, PANEL_BG);
        gfx.outline(x, y, w, h, BORDER_INNER);
        gfx.fill(x + 1, y + 1, x + w - 1, y + 2, ACCENT_GOLD_MUTED);
    }

    public static void drawCard(GuiGraphicsExtractor gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, CARD_BG);
        gfx.outline(x, y, w, h, CARD_BORDER);
    }

    public static void drawBadge(GuiGraphicsExtractor gfx, Font font, String text, int x, int y, int bgCol, int textCol) {
        int width = font.width(text) + 8;
        int height = 12;
        gfx.fill(x, y, x + width, y + height, bgCol);
        gfx.outline(x, y, width, height, (bgCol & 0x00FFFFFF) | 0x88000000);
        gfx.text(font, text, x + 4, y + 2, textCol, false);
    }

    public static void drawSectionHeader(GuiGraphicsExtractor gfx, Font font, String title, int x, int y, int width) {
        gfx.text(font, title, x, y, TEXT_GOLD, false);
        int textW = font.width(title);
        int lineX = x + textW + 8;
        if (lineX < x + width) {
            gfx.fill(lineX, y + 4, x + width, y + 5, 0x44A08A4E);
        }
    }

    public static void drawChevronIcon(GuiGraphicsExtractor gfx, int x, int y, boolean expanded, boolean hovered) {
        Identifier tex = expanded
                ? (hovered ? ICON_CHEVRON_DOWN_GOLD : ICON_CHEVRON_DOWN)
                : (hovered ? ICON_CHEVRON_RIGHT_GOLD : ICON_CHEVRON_RIGHT);
        gfx.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, 16, 16, 16, 16);
    }
}
