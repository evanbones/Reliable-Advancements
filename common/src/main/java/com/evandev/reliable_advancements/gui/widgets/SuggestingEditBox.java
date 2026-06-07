package com.evandev.reliable_advancements.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
                this.setValue(currentSuggestions.get(suggestionIndex));
                this.moveCursorToEnd(false);
                currentSuggestions = List.of();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractWidgetRenderState(gfx, mouseX, mouseY, partialTick);

        if (this.isFocused() && !currentSuggestions.isEmpty()) {
            int dropX = this.getX();
            int dropY = this.getY() + this.getHeight();
            int dropW = Math.max(this.getWidth(), 200);
            int dropH = currentSuggestions.size() * 14 + 4;

            gfx.pose().pushMatrix();
            gfx.pose().translate(0, 0, 500);
            gfx.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF0101010);
            gfx.outline(dropX, dropY, dropW, dropH, 0xFFC8AA64);

            for (int i = 0; i < currentSuggestions.size(); i++) {
                int color = (i == suggestionIndex) ? 0xFF3A3A3A : 0xFFA08060;
                if (i == suggestionIndex) {
                    gfx.fill(dropX + 1, dropY + 2 + i * 14, dropX + dropW - 1, dropY + 2 + (i + 1) * 14, 0xFFC8AA64);
                }
                gfx.text(Minecraft.getInstance().font, currentSuggestions.get(i), dropX + 4, dropY + 5 + i * 14, color, false);
            }
            gfx.pose().popMatrix();
        }
    }
}