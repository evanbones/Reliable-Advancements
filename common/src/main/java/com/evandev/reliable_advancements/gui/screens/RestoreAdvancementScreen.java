package com.evandev.reliable_advancements.gui.screens;

import com.evandev.reliable_advancements.client.ClientTabStore;
import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import com.evandev.reliable_advancements.gui.widgets.ModernButton;
import com.evandev.reliable_advancements.network.AdvancementBatchPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.tabs.TabStore;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RestoreAdvancementScreen extends Screen {
    private static final int CARD_HEIGHT = 30;
    private static final int PADDING = 10;
    private static final int MAX_VISIBLE_ROWS = 5;

    private final EnhancedAdvancementsScreen parentScreen;
    private final List<Entry> entries = new ArrayList<>();
    private final Set<ResourceLocation> selected = new LinkedHashSet<>();

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private int scrollOffset = 0;

    private ModernButton restoreBtn;
    private ModernButton permanentDeleteBtn;

    public RestoreAdvancementScreen(EnhancedAdvancementsScreen parentScreen) {
        super(Component.translatable("gui.reliable_advancements.restore_advancement.title"));
        this.parentScreen = parentScreen;

        ResourceLocation activeTabId = parentScreen.selectedTab != null ? parentScreen.selectedTab.getId() : null;
        for (Map.Entry<ResourceLocation, TabStore.Deletion> tombstone
                : ClientTabStore.restorableAdvancements(activeTabId).entrySet()) {
            entries.add(new Entry(
                    tombstone.getKey(),
                    titleOf(tombstone.getKey(), tombstone.getValue()),
                    iconOf(tombstone.getValue())
            ));
        }
    }

    private static Component titleOf(ResourceLocation id, TabStore.Deletion deletion) {
        if (deletion.titleJson() == null) return Component.literal(id.getPath());
        try {
            return ComponentSerialization.CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(deletion.titleJson()))
                    .result()
                    .orElseGet(() -> Component.literal(id.getPath()));
        } catch (Exception e) {
            return Component.literal(id.getPath());
        }
    }

    private static ItemStack iconOf(TabStore.Deletion deletion) {
        if (deletion.icon() == null) return new ItemStack(Items.STONE);
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(deletion.icon()).orElse(Items.STONE));
    }

    @Override
    protected void init() {
        this.windowWidth = 380;
        int listHeight = Math.min(entries.size(), MAX_VISIBLE_ROWS) * (CARD_HEIGHT + 4);
        this.windowHeight = Math.max(160, 80 + (entries.isEmpty() ? 30 : listHeight) + 40);
        this.windowX = (this.width - this.windowWidth) / 2;
        this.windowY = (this.height - this.windowHeight) / 2;

        int btnY = this.windowY + this.windowHeight - 32;

        if (!entries.isEmpty()) {
            this.restoreBtn = new ModernButton(
                    this.windowX + this.windowWidth - 85 - PADDING, btnY, 85, 22,
                    Component.translatable("gui.reliable_advancements.restore_advancement.restore_button"),
                    btn -> restoreSelected(),
                    ModernButton.Style.PRIMARY
            );
            this.restoreBtn.active = !selected.isEmpty();
            addRenderableWidget(this.restoreBtn);

            this.permanentDeleteBtn = new ModernButton(
                    this.windowX + this.windowWidth - 85 - PADDING - 6 - 145, btnY, 145, 22,
                    Component.translatable("gui.reliable_advancements.restore_advancement.permanent_delete_button"),
                    btn -> permanentlyDeleteSelected(),
                    ModernButton.Style.DANGER
            );
            this.permanentDeleteBtn.active = !selected.isEmpty();
            addRenderableWidget(this.permanentDeleteBtn);

            addRenderableWidget(new ModernButton(
                    this.windowX + PADDING, btnY, 70, 22,
                    Component.translatable("gui.cancel"),
                    btn -> onClose(),
                    ModernButton.Style.SECONDARY
            ));
        } else {
            addRenderableWidget(new ModernButton(
                    this.windowX + (this.windowWidth - 100) / 2, btnY, 100, 22,
                    Component.translatable("gui.ok"),
                    btn -> onClose(),
                    ModernButton.Style.SECONDARY
            ));
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parentScreen);
    }

    private void restoreSelected() {
        if (selected.isEmpty()) return;

        Services.PLATFORM.sendAdvancementBatch(new AdvancementBatchPayload(
                AdvancementBatchPayload.Op.RESTORE, new ArrayList<>(selected)));
        parentScreen.setLoading(true);
        Minecraft.getInstance().setScreen(parentScreen);
    }

    private void permanentlyDeleteSelected() {
        if (selected.isEmpty()) return;

        boolean single = selected.size() == 1;
        String suffix = single ? "" : "_multiple";
        Component title = single
                ? Component.translatable("gui.reliable_advancements.dialog.permanent_delete_advancement.title")
                : Component.translatable("gui.reliable_advancements.dialog.permanent_delete_advancement" + suffix + ".title", selected.size());
        Component message = single
                ? Component.translatable("gui.reliable_advancements.dialog.permanent_delete_advancement.message")
                : Component.translatable("gui.reliable_advancements.dialog.permanent_delete_advancement" + suffix + ".message", selected.size());

        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        Services.PLATFORM.sendAdvancementBatch(new AdvancementBatchPayload(
                                AdvancementBatchPayload.Op.PERMANENT_DELETE, new ArrayList<>(selected)));
                        parentScreen.setLoading(true);
                        Minecraft.getInstance().setScreen(parentScreen);
                    } else {
                        Minecraft.getInstance().setScreen(this);
                    }
                },
                title,
                message
        ));
    }

    private int listTop() {
        return this.windowY + 42;
    }

    private @Nullable Entry rowAt(double mouseX, double mouseY) {
        int listW = this.windowWidth - PADDING * 2;
        if (mouseX < this.windowX + PADDING || mouseX > this.windowX + PADDING + listW) return null;

        int visibleCount = Math.min(entries.size(), MAX_VISIBLE_ROWS);
        for (int i = 0; i < visibleCount; i++) {
            int index = i + scrollOffset;
            if (index >= entries.size()) break;

            int rowY = listTop() + i * (CARD_HEIGHT + 4);
            if (mouseY >= rowY && mouseY <= rowY + CARD_HEIGHT) return entries.get(index);
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, entries.size() - MAX_VISIBLE_ROWS);
        int listBottom = listTop() + Math.min(entries.size(), MAX_VISIBLE_ROWS) * (CARD_HEIGHT + 4);
        boolean overList = mouseX >= this.windowX + PADDING
                && mouseX <= this.windowX + this.windowWidth - PADDING
                && mouseY >= listTop() && mouseY <= listBottom;

        if (maxScroll > 0 && overList) {
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Entry clicked = rowAt(mouseX, mouseY);
            if (clicked != null) {
                if (!selected.remove(clicked.id())) selected.add(clicked.id());
                boolean hasSelection = !selected.isEmpty();
                if (this.restoreBtn != null) this.restoreBtn.active = hasSelection;
                if (this.permanentDeleteBtn != null) this.permanentDeleteBtn.active = hasSelection;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, EditorTheme.BG_OVERLAY);
        EditorTheme.drawWindow(gfx, this.windowX, this.windowY, this.windowWidth, this.windowHeight);

        gfx.drawString(this.font, this.title, this.windowX + PADDING, this.windowY + 12, EditorTheme.TEXT_GOLD, false);

        int listW = this.windowWidth - PADDING * 2;

        if (entries.isEmpty()) {
            Component emptyText = Component.translatable("gui.reliable_advancements.restore_advancement.empty");
            int textW = this.font.width(emptyText);
            gfx.drawString(this.font, emptyText, this.windowX + (this.windowWidth - textW) / 2,
                    listTop() + 14, EditorTheme.TEXT_MUTED, false);
        } else {
            Component subtitle = Component.translatable("gui.reliable_advancements.restore_advancement.subtitle");
            gfx.drawString(this.font, subtitle, this.windowX + PADDING, this.windowY + 28, EditorTheme.TEXT_MUTED, false);

            Entry hovered = rowAt(mouseX, mouseY);
            int visibleCount = Math.min(entries.size(), MAX_VISIBLE_ROWS);
            for (int i = 0; i < visibleCount; i++) {
                int index = i + scrollOffset;
                if (index >= entries.size()) break;

                Entry entry = entries.get(index);
                int rowY = listTop() + i * (CARD_HEIGHT + 4);
                boolean isSelected = selected.contains(entry.id());
                boolean isHovered = entry == hovered;

                int bg = isSelected ? EditorTheme.TAB_ACTIVE_BG : (isHovered ? EditorTheme.TAB_HOVER_BG : EditorTheme.CARD_BG);
                int border = isSelected ? EditorTheme.ACCENT_GOLD : (isHovered ? EditorTheme.BORDER_INNER : EditorTheme.CARD_BORDER);

                gfx.fill(this.windowX + PADDING, rowY, this.windowX + PADDING + listW, rowY + CARD_HEIGHT, bg);
                gfx.renderOutline(this.windowX + PADDING, rowY, listW, CARD_HEIGHT, border);

                if (isSelected) {
                    gfx.fill(this.windowX + PADDING, rowY, this.windowX + PADDING + 3, rowY + CARD_HEIGHT, EditorTheme.ACCENT_GOLD);
                }

                gfx.renderFakeItem(entry.icon(), this.windowX + PADDING + 8, rowY + 7);
                gfx.drawString(this.font, entry.title(), this.windowX + PADDING + 32, rowY + 4,
                        isSelected ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_PRIMARY, false);
                gfx.drawString(this.font, entry.id().toString(), this.windowX + PADDING + 32, rowY + 17,
                        EditorTheme.TEXT_MUTED, false);
            }
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private record Entry(ResourceLocation id, Component title, ItemStack icon) {
    }
}
