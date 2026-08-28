package com.evandev.reliable_advancements.gui.widgets;

import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SuggestingEditBox extends ModernEditBox {
    private static final int ROW_HEIGHT = 18;
    private static final int MAX_VISIBLE = 8;

    private final Supplier<List<String>> suggestionSupplier;
    private @Nullable Function<String, ItemStack> iconResolver;
    private List<String> currentSuggestions = List.of();
    private int suggestionIndex = -1;
    private int scrollOffset = 0;
    private Consumer<String> externalResponder;

    public SuggestingEditBox(Font font, int x, int y, int width, int height, Component message, Supplier<List<String>> suggestionSupplier) {
        this(font, x, y, width, height, message, suggestionSupplier, null);
    }

    public SuggestingEditBox(Font font, int x, int y, int width, int height, Component message, Supplier<List<String>> suggestionSupplier, @Nullable Function<String, ItemStack> iconResolver) {
        super(font, x, y, width, height, message);
        this.suggestionSupplier = suggestionSupplier;
        this.iconResolver = iconResolver;
        super.setResponder(this::internalResponder);
    }

    public static ItemStack defaultItemIconResolver(String suggestion) {
        if (suggestion == null || suggestion.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation loc = ResourceLocation.tryParse(suggestion);
        if (loc == null) return ItemStack.EMPTY;

        if (BuiltInRegistries.ITEM.containsKey(loc)) {
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item != Items.AIR) return new ItemStack(item);
        }

        if (BuiltInRegistries.BLOCK.containsKey(loc)) {
            Item item = BuiltInRegistries.BLOCK.get(loc).asItem();
            if (item != Items.AIR) return new ItemStack(item);
        }

        return ItemStack.EMPTY;
    }

    // these are just for fun tbh
    public static ItemStack lootTableIconResolver(String suggestion) {
        if (suggestion == null || suggestion.isEmpty()) return ItemStack.EMPTY;

        ItemStack directItem = defaultItemIconResolver(suggestion);
        if (!directItem.isEmpty()) return directItem;

        ResourceLocation loc = ResourceLocation.tryParse(suggestion);
        if (loc == null) return ItemStack.EMPTY;

        String path = loc.getPath();

        if (path.startsWith("blocks/")) {
            String blockPath = path.substring("blocks/".length());
            ResourceLocation blockLoc = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), blockPath);
            if (BuiltInRegistries.BLOCK.containsKey(blockLoc)) {
                Item item = BuiltInRegistries.BLOCK.get(blockLoc).asItem();
                if (item != Items.AIR) return new ItemStack(item);
            }
        }

        if (path.startsWith("chests/")) {
            return new ItemStack(Items.CHEST);
        }

        if (path.startsWith("gameplay/fishing")) {
            return new ItemStack(Items.FISHING_ROD);
        }
        if (path.startsWith("gameplay/hero_of_the_village")) {
            return new ItemStack(Items.EMERALD);
        }
        if (path.startsWith("gameplay/cat_morning_gift")) {
            return new ItemStack(Items.STRING);
        }
        if (path.startsWith("gameplay/sniffer_digging")) {
            return new ItemStack(Items.SNIFFER_EGG);
        }
        if (path.startsWith("gameplay/piglin_bartering")) {
            return new ItemStack(Items.GOLD_INGOT);
        }
        if (path.startsWith("archaeology/")) {
            return new ItemStack(Items.BRUSH);
        }
        if (path.startsWith("pots/")) {
            return new ItemStack(Items.DECORATED_POT);
        }
        if (path.startsWith("spawners/") || path.startsWith("dispensers/trial_chambers") || path.startsWith("equipment/trial_chamber")) {
            return new ItemStack(Items.TRIAL_KEY);
        }
        if (path.startsWith("shearing/")) {
            return new ItemStack(Items.SHEARS);
        }
        if (path.startsWith("entities/")) {
            return new ItemStack(Items.SPAWNER);
        }

        return ItemStack.EMPTY;
    }

    public void setIconResolver(@Nullable Function<String, ItemStack> iconResolver) {
        this.iconResolver = iconResolver;
    }

    public boolean hasSuggestions() {
        return this.isFocused() && !currentSuggestions.isEmpty();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.currentSuggestions = List.of();
            this.suggestionIndex = -1;
            this.scrollOffset = 0;
        } else {
            updateSuggestions(this.getValue());
        }
    }

    @Override
    public void setResponder(@NotNull Consumer<String> responder) {
        this.externalResponder = responder;
    }

    private void internalResponder(String text) {
        if (this.externalResponder != null) {
            this.externalResponder.accept(text);
        }
        if (!this.isFocused()) {
            currentSuggestions = List.of();
            suggestionIndex = -1;
            scrollOffset = 0;
            return;
        }
        updateSuggestions(text);
    }

    public void updateSuggestions(String text) {
        List<String> allOptions = suggestionSupplier.get();
        if (allOptions == null || allOptions.isEmpty()) {
            currentSuggestions = List.of();
            suggestionIndex = -1;
            scrollOffset = 0;
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            currentSuggestions = allOptions.stream().limit(100).collect(Collectors.toList());
        } else {
            String lower = text.trim().toLowerCase();
            currentSuggestions = allOptions.stream()
                    .filter(id -> id.toLowerCase().contains(lower))
                    .sorted((a, b) -> {
                        String aLower = a.toLowerCase();
                        String bLower = b.toLowerCase();
                        boolean aStarts = aLower.startsWith(lower) || aLower.substring(aLower.indexOf(':') + 1).startsWith(lower);
                        boolean bStarts = bLower.startsWith(lower) || bLower.substring(bLower.indexOf(':') + 1).startsWith(lower);
                        if (aStarts != bStarts) return aStarts ? -1 : 1;
                        return a.compareToIgnoreCase(b);
                    })
                    .limit(100)
                    .collect(Collectors.toList());
        }
        suggestionIndex = currentSuggestions.isEmpty() ? -1 : 0;
        scrollOffset = 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isFocused() && !currentSuggestions.isEmpty()) {
            if (keyCode == 256) { // ESCAPE
                currentSuggestions = List.of();
                suggestionIndex = -1;
                scrollOffset = 0;
                return true;
            } else if (keyCode == 264) { // DOWN
                suggestionIndex = (suggestionIndex + 1) % currentSuggestions.size();
                if (suggestionIndex >= scrollOffset + MAX_VISIBLE) {
                    scrollOffset = suggestionIndex - MAX_VISIBLE + 1;
                } else if (suggestionIndex < scrollOffset) {
                    scrollOffset = suggestionIndex;
                }
                return true;
            } else if (keyCode == 265) { // UP
                suggestionIndex = (suggestionIndex - 1 + currentSuggestions.size()) % currentSuggestions.size();
                if (suggestionIndex < scrollOffset) {
                    scrollOffset = suggestionIndex;
                } else if (suggestionIndex >= scrollOffset + MAX_VISIBLE) {
                    scrollOffset = suggestionIndex - MAX_VISIBLE + 1;
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335 || keyCode == 258) { // ENTER, NUMPAD ENTER or TAB
                applySuggestion(suggestionIndex);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isFocused() && currentSuggestions.size() > MAX_VISIBLE) {
            int dropX = this.getX();
            int dropY = this.getY() + this.getHeight() + 1;
            int dropW = Math.max(this.getWidth(), 240);
            int dropH = Math.min(currentSuggestions.size(), MAX_VISIBLE) * ROW_HEIGHT + 4;

            if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY && mouseY <= dropY + dropH) {
                int maxScroll = currentSuggestions.size() - MAX_VISIBLE;
                scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && tryClickSuggestion(mouseX, mouseY)) {
            return true;
        }
        boolean inside = mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= getY() && mouseY <= getY() + getHeight();
        if (!inside && isFocused()) {
            setFocused(false);
            currentSuggestions = List.of();
            suggestionIndex = -1;
            scrollOffset = 0;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean tryClickSuggestion(double mouseX, double mouseY) {
        if (!this.isFocused() || currentSuggestions.isEmpty()) return false;

        int dropX = this.getX();
        int dropY = this.getY() + this.getHeight();
        int dropW = Math.max(this.getWidth(), 240);
        int visibleCount = Math.min(currentSuggestions.size(), MAX_VISIBLE);
        int dropH = visibleCount * ROW_HEIGHT + 4;

        if (mouseX < dropX || mouseX >= dropX + dropW || mouseY < dropY || mouseY >= dropY + dropH) {
            return false;
        }

        int index = (int) ((mouseY - dropY - 2) / ROW_HEIGHT) + scrollOffset;
        if (index < 0 || index >= currentSuggestions.size()) return false;

        applySuggestion(index);
        return true;
    }

    private void applySuggestion(int index) {
        if (index < 0 || index >= currentSuggestions.size()) return;

        this.setValue(currentSuggestions.get(index));
        this.setCursorPosition(0);
        this.setHighlightPos(0);
        currentSuggestions = List.of();
        suggestionIndex = -1;
        scrollOffset = 0;
    }

    private int hoveredSuggestion(int mouseX, int mouseY) {
        if (!this.isFocused() || currentSuggestions.isEmpty()) return -1;
        int dropX = this.getX();
        int dropY = this.getY() + this.getHeight();
        int dropW = Math.max(this.getWidth(), 240);
        int visibleCount = Math.min(currentSuggestions.size(), MAX_VISIBLE);
        int dropH = visibleCount * ROW_HEIGHT + 4;

        if (mouseX < dropX || mouseX >= dropX + dropW || mouseY < dropY || mouseY >= dropY + dropH) return -1;

        int index = ((mouseY - dropY - 2) / ROW_HEIGHT) + scrollOffset;
        return index >= 0 && index < currentSuggestions.size() ? index : -1;
    }

    private ItemStack resolveSuggestionIcon(String suggestion) {
        if (iconResolver != null) {
            ItemStack stack = iconResolver.apply(suggestion);
            return stack != null ? stack : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    public void renderSuggestionsPopup(GuiGraphics gfx, int mouseX, int mouseY) {
        if (!this.isFocused() || currentSuggestions.isEmpty()) return;

        int dropX = this.getX();
        int dropY = this.getY() + this.getHeight() + 1;
        int dropW = Math.max(this.getWidth(), 240);
        int visibleCount = Math.min(currentSuggestions.size(), MAX_VISIBLE);
        int dropH = visibleCount * ROW_HEIGHT + 4;
        int hovered = hoveredSuggestion(mouseX, mouseY);

        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 500);
        gfx.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF4141620);
        gfx.renderOutline(dropX, dropY, dropW, dropH, EditorTheme.ACCENT_GOLD_MUTED);

        for (int i = 0; i < visibleCount; i++) {
            int suggIdx = i + scrollOffset;
            if (suggIdx >= currentSuggestions.size()) break;

            boolean highlighted = (suggIdx == suggestionIndex || suggIdx == hovered);
            int color = highlighted ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
            if (highlighted) {
                gfx.fill(dropX + 1, dropY + 2 + i * ROW_HEIGHT, dropX + dropW - 1, dropY + 2 + (i + 1) * ROW_HEIGHT, 0xFF2A3042);
            }

            String suggestion = currentSuggestions.get(suggIdx);
            ItemStack stack = resolveSuggestionIcon(suggestion);

            if (!stack.isEmpty()) {
                gfx.renderFakeItem(stack, dropX + 3, dropY + 2 + i * ROW_HEIGHT + 1);
                gfx.drawString(Minecraft.getInstance().font, suggestion, dropX + 23, dropY + 2 + i * ROW_HEIGHT + 5, color, false);
            } else {
                gfx.drawString(Minecraft.getInstance().font, suggestion, dropX + 6, dropY + 2 + i * ROW_HEIGHT + 5, color, false);
            }
        }

        if (currentSuggestions.size() > MAX_VISIBLE) {
            int maxScroll = currentSuggestions.size() - MAX_VISIBLE;
            int barH = dropH - 4;
            int thumbH = Math.max(8, barH * MAX_VISIBLE / currentSuggestions.size());
            int thumbY = dropY + 2 + (barH - thumbH) * scrollOffset / maxScroll;
            gfx.fill(dropX + dropW - 3, dropY + 2, dropX + dropW - 1, dropY + dropH - 2, 0x33FFFFFF);
            gfx.fill(dropX + dropW - 3, thumbY, dropX + dropW - 1, thumbY + thumbH, EditorTheme.ACCENT_GOLD);
        }

        gfx.pose().popPose();
    }
}