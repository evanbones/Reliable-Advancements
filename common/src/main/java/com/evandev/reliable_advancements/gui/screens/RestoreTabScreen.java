package com.evandev.reliable_advancements.gui.screens;

import com.evandev.reliable_advancements.client.ClientTabStore;
import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import com.evandev.reliable_advancements.gui.widgets.ModernButton;
import com.evandev.reliable_advancements.network.TabActionPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.tabs.ResolvedTab;
import com.evandev.reliable_advancements.tabs.TabDefinition;
import com.evandev.reliable_advancements.tabs.TabResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RestoreTabScreen extends Screen {
    private static final int CARD_HEIGHT = 38;
    private static final int PADDING = 10;
    private static final int MAX_VISIBLE_ROWS = 5;

    private final EnhancedAdvancementsScreen parentScreen;
    private final List<RestorableTabEntry> restorableTabs = new ArrayList<>();

    private int windowX;
    private int windowY;
    private int windowWidth;
    private int windowHeight;
    private int scrollOffset = 0;
    private @Nullable RestorableTabEntry selectedEntry = null;

    private ModernButton restoreBtn;
    private ModernButton permanentDeleteBtn;

    public RestoreTabScreen(EnhancedAdvancementsScreen parentScreen) {
        super(Component.translatable("gui.reliable_advancements.restore_tab.title"));
        this.parentScreen = parentScreen;

        for (TabDefinition def : ClientTabStore.restorable()) {
            ResolvedTab described = TabResolver.describe(def, ClientTabStore.get());
            restorableTabs.add(new RestorableTabEntry(def.id, described.title().getString(), described.icon()));
        }
    }

    @Override
    protected void init() {
        this.windowWidth = 380;
        int listHeight = Math.min(restorableTabs.size(), MAX_VISIBLE_ROWS) * (CARD_HEIGHT + 4);
        this.windowHeight = Math.max(160, 80 + (restorableTabs.isEmpty() ? 30 : listHeight) + 40);
        this.windowX = (this.width - this.windowWidth) / 2;
        this.windowY = (this.height - this.windowHeight) / 2;

        int btnY = this.windowY + this.windowHeight - 32;

        if (!restorableTabs.isEmpty()) {
            this.restoreBtn = new ModernButton(
                    this.windowX + this.windowWidth - 85 - PADDING, btnY, 85, 22,
                    Component.translatable("gui.reliable_advancements.restore_tab.restore_button"),
                    btn -> {
                        if (selectedEntry != null) restoreTab(selectedEntry);
                    },
                    ModernButton.Style.PRIMARY
            );
            this.restoreBtn.active = selectedEntry != null;
            addRenderableWidget(this.restoreBtn);

            this.permanentDeleteBtn = new ModernButton(
                    this.windowX + this.windowWidth - 85 - PADDING - 6 - 145, btnY, 145, 22,
                    Component.translatable("gui.reliable_advancements.restore_tab.permanent_delete_button"),
                    btn -> {
                        if (selectedEntry != null) permanentlyDeleteTab(selectedEntry);
                    },
                    ModernButton.Style.DANGER
            );
            this.permanentDeleteBtn.active = selectedEntry != null;
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

    private void restoreTab(RestorableTabEntry entry) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        EnhancedAdvancementsScreen.setSavedSelectedTab(entry.id());
                        Services.PLATFORM.sendTabAction(
                                TabActionPayload.of(TabActionPayload.Action.RESTORE, entry.id()));
                        parentScreen.setLoading(true);
                        Minecraft.getInstance().setScreen(parentScreen);
                    } else {
                        Minecraft.getInstance().setScreen(this);
                    }
                },
                Component.translatable("gui.reliable_advancements.restore_tab.confirm.title", entry.title()),
                Component.translatable("gui.reliable_advancements.restore_tab.confirm.message")
        ));
    }

    private void permanentlyDeleteTab(RestorableTabEntry entry) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        Services.PLATFORM.sendTabAction(
                                TabActionPayload.of(TabActionPayload.Action.PERMANENT_DELETE, entry.id()));
                        parentScreen.setLoading(true);
                        Minecraft.getInstance().setScreen(parentScreen);
                    } else {
                        Minecraft.getInstance().setScreen(this);
                    }
                },
                Component.translatable("gui.reliable_advancements.dialog.permanent_delete_tab.title", entry.title()),
                Component.translatable("gui.reliable_advancements.dialog.permanent_delete_tab.message")
        ));
    }

    private boolean isOverList(double mouseX, double mouseY) {
        int listTop = this.windowY + 42;
        int listBottom = listTop + Math.min(restorableTabs.size(), MAX_VISIBLE_ROWS) * (CARD_HEIGHT + 4);
        return mouseX >= this.windowX + PADDING && mouseX <= this.windowX + this.windowWidth - PADDING
                && mouseY >= listTop && mouseY <= listBottom;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, restorableTabs.size() - MAX_VISIBLE_ROWS);
        if (maxScroll > 0 && isOverList(mouseX, mouseY)) {
            this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int listY = this.windowY + 42;
            int listW = this.windowWidth - PADDING * 2;

            int visibleCount = Math.min(restorableTabs.size(), MAX_VISIBLE_ROWS);
            for (int i = 0; i < visibleCount; i++) {
                int index = i + scrollOffset;
                if (index >= restorableTabs.size()) break;

                int rowY = listY + i * (CARD_HEIGHT + 4);
                if (mouseX >= this.windowX + PADDING && mouseX <= this.windowX + PADDING + listW
                        && mouseY >= rowY && mouseY <= rowY + CARD_HEIGHT) {
                    this.selectedEntry = restorableTabs.get(index);
                    if (this.restoreBtn != null) {
                        this.restoreBtn.active = true;
                    }
                    if (this.permanentDeleteBtn != null) {
                        this.permanentDeleteBtn.active = true;
                    }
                    return true;
                }
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

        int listY = this.windowY + 42;
        int listW = this.windowWidth - PADDING * 2;

        if (restorableTabs.isEmpty()) {
            Component emptyText = Component.translatable("gui.reliable_advancements.restore_tab.empty");
            int textW = this.font.width(emptyText);
            gfx.drawString(this.font, emptyText, this.windowX + (this.windowWidth - textW) / 2, listY + 14, EditorTheme.TEXT_MUTED, false);
        } else {
            Component subtitle = Component.translatable("gui.reliable_advancements.restore_tab.subtitle");
            gfx.drawString(this.font, subtitle, this.windowX + PADDING, this.windowY + 28, EditorTheme.TEXT_MUTED, false);

            int visibleCount = Math.min(restorableTabs.size(), MAX_VISIBLE_ROWS);
            for (int i = 0; i < visibleCount; i++) {
                int index = i + scrollOffset;
                if (index >= restorableTabs.size()) break;

                RestorableTabEntry entry = restorableTabs.get(index);
                int rowY = listY + i * (CARD_HEIGHT + 4);
                boolean isSelected = entry == selectedEntry;
                boolean isHovered = mouseX >= this.windowX + PADDING && mouseX <= this.windowX + PADDING + listW
                        && mouseY >= rowY && mouseY <= rowY + CARD_HEIGHT;

                int bg = isSelected ? EditorTheme.TAB_ACTIVE_BG : (isHovered ? EditorTheme.TAB_HOVER_BG : EditorTheme.CARD_BG);
                int border = isSelected ? EditorTheme.ACCENT_GOLD : (isHovered ? EditorTheme.BORDER_INNER : EditorTheme.CARD_BORDER);

                gfx.fill(this.windowX + PADDING, rowY, this.windowX + PADDING + listW, rowY + CARD_HEIGHT, bg);
                gfx.renderOutline(this.windowX + PADDING, rowY, listW, CARD_HEIGHT, border);

                if (isSelected) {
                    gfx.fill(this.windowX + PADDING, rowY, this.windowX + PADDING + 3, rowY + CARD_HEIGHT, EditorTheme.ACCENT_GOLD);
                }

                gfx.renderFakeItem(entry.icon(), this.windowX + PADDING + 8, rowY + 11);

                gfx.drawString(this.font, entry.title(), this.windowX + PADDING + 32, rowY + 6, isSelected ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_PRIMARY, false);
                gfx.drawString(this.font, entry.id().toString(), this.windowX + PADDING + 32, rowY + 20, EditorTheme.TEXT_MUTED, false);
            }
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private record RestorableTabEntry(ResourceLocation id, String title, ItemStack icon) {
    }
}
