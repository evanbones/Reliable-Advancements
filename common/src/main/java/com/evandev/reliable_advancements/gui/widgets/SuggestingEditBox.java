package com.evandev.reliable_advancements.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SuggestingEditBox extends EditBox {
    private final Supplier<List<String>> suggestionSupplier;
    private List<String> currentSuggestions = List.of();
    private int suggestionIndex = -1;
    private Consumer<String> externalResponder;

    public SuggestingEditBox(Font font, int x, int y, int width, int height, Component message, Supplier<List<String>> suggestionSupplier) {
        super(font, x, y, width, height, message);
        this.suggestionSupplier = suggestionSupplier;
        super.setResponder(this::internalResponder);
    }

    @Override
    public void setResponder(@NotNull Consumer<String> responder) {
        this.externalResponder = responder;
    }

    private void internalResponder(String text) {
        if (this.externalResponder != null) {
            this.externalResponder.accept(text);
        }
        if (text.isEmpty()) {
            currentSuggestions = List.of();
            suggestionIndex = -1;
            return;
        }
        List<String> allOptions = suggestionSupplier.get();
        currentSuggestions = allOptions.stream()
                .filter(id -> id.toLowerCase().contains(text.toLowerCase()))
                .limit(6).collect(Collectors.toList());
        suggestionIndex = currentSuggestions.isEmpty() ? -1 : 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isFocused() && !currentSuggestions.isEmpty()) {
            if (keyCode == 264) { // Down arrow
                suggestionIndex = (suggestionIndex + 1) % currentSuggestions.size();
                return true;
            } else if (keyCode == 265) { // Up arrow
                suggestionIndex = (suggestionIndex - 1 + currentSuggestions.size()) % currentSuggestions.size();
                return true;
            } else if (keyCode == 257 || keyCode == 335 || keyCode == 258) { // Enter or Tab
                applySuggestion(suggestionIndex);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean tryClickSuggestion(double mouseX, double mouseY) {
        if (!this.isFocused() || currentSuggestions.isEmpty()) return false;

        int dropX = this.getX();
        int dropY = this.getY() + this.getHeight();
        int dropW = Math.max(this.getWidth(), 200);
        int dropH = currentSuggestions.size() * 14 + 4;

        if (mouseX < dropX || mouseX >= dropX + dropW || mouseY < dropY || mouseY >= dropY + dropH) {
            return false;
        }

        int index = (int) ((mouseY - dropY - 2) / 14);
        if (index < 0 || index >= currentSuggestions.size()) return false;

        applySuggestion(index);
        return true;
    }

    private void applySuggestion(int index) {
        if (index < 0 || index >= currentSuggestions.size()) return;

        this.setValue(currentSuggestions.get(index));
        this.moveCursorToEnd(false);
        currentSuggestions = List.of();
        suggestionIndex = -1;
    }

    private int hoveredSuggestion(int mouseX, int mouseY) {
        int dropX = this.getX();
        int dropY = this.getY() + this.getHeight();
        int dropW = Math.max(this.getWidth(), 200);

        if (mouseX < dropX || mouseX >= dropX + dropW) return -1;

        int index = (mouseY - dropY - 2) / 14;
        return index >= 0 && index < currentSuggestions.size() ? index : -1;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(gfx, mouseX, mouseY, partialTick);

        if (this.isFocused() && !currentSuggestions.isEmpty()) {
            int dropX = this.getX();
            int dropY = this.getY() + this.getHeight();
            int dropW = Math.max(this.getWidth(), 200);
            int dropH = currentSuggestions.size() * 14 + 4;
            int hovered = hoveredSuggestion(mouseX, mouseY);

            gfx.pose().pushPose();
            gfx.pose().translate(0, 0, 500);
            gfx.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF0101010);
            gfx.renderOutline(dropX, dropY, dropW, dropH, 0xFFC8AA64);

            for (int i = 0; i < currentSuggestions.size(); i++) {
                boolean highlighted = (i == suggestionIndex || i == hovered);
                int color = highlighted ? 0xFF3A3A3A : 0xFFA08060;
                if (highlighted) {
                    gfx.fill(dropX + 1, dropY + 2 + i * 14, dropX + dropW - 1, dropY + 2 + (i + 1) * 14, 0xFFC8AA64);
                }
                gfx.drawString(Minecraft.getInstance().font, currentSuggestions.get(i), dropX + 4, dropY + 5 + i * 14, color, false);
            }
            gfx.pose().popPose();
        }
    }
}