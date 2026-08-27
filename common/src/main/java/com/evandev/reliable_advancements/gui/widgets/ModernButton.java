package com.evandev.reliable_advancements.gui.widgets;

import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModernButton extends Button {
    private Style style;

    public ModernButton(int x, int y, int width, int height, Component message, OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    public static Builder modernBuilder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        boolean hovered = this.isHoveredOrFocused();

        int bgCol;
        int borderCol;
        int textCol;

        switch (style) {
            case PRIMARY -> {
                bgCol = hovered ? 0xFF2D3548 : 0xFF202636;
                borderCol = hovered ? EditorTheme.ACCENT_GOLD : EditorTheme.ACCENT_GOLD_MUTED;
                textCol = hovered ? 0xFFFFFFFF : EditorTheme.TEXT_GOLD;
            }
            case DANGER -> {
                bgCol = hovered ? 0xFF4A1818 : 0xFF221619;
                borderCol = hovered ? EditorTheme.TEXT_RED : 0xFF5C2424;
                textCol = hovered ? 0xFFFFFFFF : EditorTheme.TEXT_RED;
            }
            case TOGGLE_ON -> {
                bgCol = hovered ? 0xFF1B3822 : 0xFF142919;
                borderCol = hovered ? 0xFF51CF66 : 0xFF2F8540;
                textCol = EditorTheme.TEXT_GREEN;
            }
            case TOGGLE_OFF -> {
                bgCol = hovered ? 0xFF282C3A : 0xFF1A1C26;
                borderCol = hovered ? 0xFF5C637E : 0xFF35394A;
                textCol = EditorTheme.TEXT_MUTED;
            }
            default -> {
                bgCol = hovered ? 0xFF2A2E3D : 0xFF1B1E29;
                borderCol = hovered ? 0xFF5C668A : 0xFF363B4F;
                textCol = hovered ? 0xFFFFFFFF : EditorTheme.TEXT_LABEL;
            }
        }

        if (!this.active) {
            bgCol = 0xFF14151C;
            borderCol = 0xFF252733;
            textCol = 0xFF4E5266;
        }

        gfx.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), bgCol);
        gfx.renderOutline(getX(), getY(), getWidth(), getHeight(), borderCol);

        int textX = getX() + (getWidth() - font.width(getMessage())) / 2;
        int textY = getY() + (getHeight() - font.lineHeight) / 2;
        gfx.drawString(font, getMessage(), textX, textY, textCol, false);
    }

    public enum Style {
        PRIMARY,
        SECONDARY,
        DANGER,
        TOGGLE_ON,
        TOGGLE_OFF
    }

    public static class Builder {
        private final Component message;
        private final OnPress onPress;
        private @Nullable Tooltip tooltip;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private Style style = Style.SECONDARY;

        public Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder tooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public ModernButton build() {
            ModernButton button = new ModernButton(this.x, this.y, this.width, this.height, this.message, this.onPress, this.style);
            button.setTooltip(this.tooltip);
            return button;
        }
    }
}
