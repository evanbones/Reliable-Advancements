package com.evandev.reliable_advancements.gui.widgets;

import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ModernEditBox extends EditBox {
    private static final int PAD_X = 6;
    private boolean hasError = false;

    public ModernEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.setBordered(false);
        this.setTextColor(EditorTheme.TEXT_PRIMARY);
        this.setTextColorUneditable(EditorTheme.TEXT_MUTED);
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public boolean hasError() {
        return hasError;
    }

    @Override
    public void setValue(@NotNull String text) {
        super.setValue(text);
        this.setCursorPosition(0);
        this.setHighlightPos(0);
    }

    @Override
    public int getInnerWidth() {
        return Math.max(1, this.getWidth() - PAD_X * 2);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX - PAD_X, mouseY);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) return;

        gfx.fill(getX(), getY(), getX() + width, getY() + height, 0xF411131A);

        int borderColor;
        if (hasError) {
            borderColor = EditorTheme.BORDER_ERROR;
        } else if (this.isFocused()) {
            borderColor = EditorTheme.BORDER_FOCUSED;
        } else if (isHoveredOrFocused()) {
            borderColor = 0xFF4A5578;
        } else {
            borderColor = EditorTheme.BORDER_INNER;
        }
        gfx.renderOutline(getX(), getY(), width, height, borderColor);

        int padY = Math.max(0, (height - 9) / 2);
        gfx.pose().pushPose();
        gfx.pose().translate(PAD_X, padY, 0);
        super.renderWidget(gfx, mouseX - PAD_X, mouseY - padY, partialTick);
        gfx.pose().popPose();
    }
}
