package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.gui.screens.RestoreAdvancementScreen;
import com.evandev.reliable_advancements.gui.screens.RestoreTabScreen;
import com.evandev.reliable_advancements.network.RequestAdvancementJsonPayload;
import com.evandev.reliable_advancements.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdvancementContextMenu {
    private final EnhancedAdvancementsScreen parentScreen;
    private final @Nullable EnhancedAdvancementWidget widget;
    private final List<ContextOption> options = new ArrayList<>();

    private int x, y;
    private int width;
    private int height;

    public AdvancementContextMenu(EnhancedAdvancementsScreen parentScreen, @Nullable EnhancedAdvancementWidget widget, int mouseX, int mouseY) {
        this.parentScreen = parentScreen;
        this.widget = widget;

        if (widget != null) {
            buildAdvancementOptions();
        } else {
            buildBackgroundOptions(mouseX, mouseY);
        }
        layOut(mouseX, mouseY);
    }

    public AdvancementContextMenu(EnhancedAdvancementsScreen parentScreen, EnhancedAdvancementTab tab, int mouseX, int mouseY) {
        this.parentScreen = parentScreen;
        this.widget = null;

        buildTabOptions(tab);
        layOut(mouseX, mouseY);
    }

    private void layOut(int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        int maxW = 160;
        for (ContextOption opt : this.options) {
            maxW = Math.max(maxW, font.width(opt.label()) + 16);
        }
        this.width = maxW;
        this.height = this.options.size() * 20 + 4;

        this.x = Math.min(mouseX, parentScreen.width - this.width - 5);
        this.y = Math.min(mouseY, parentScreen.height - this.height - 5);
    }

    private void buildAdvancementOptions() {
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.edit_properties"), false, () -> openEditor("Properties")));
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.copy"), false, () -> {
            parentScreen.copyAdvancement(widget);
            parentScreen.closeContextMenu();
        }));
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.link_parents"), false, () -> parentScreen.startLinking(widget)));
        if (!widget.getParents().isEmpty()) {
            this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.unlink_all_parents"), false, () -> {
                parentScreen.unlinkAllParents(widget);
                parentScreen.closeContextMenu();
            }));
        }

        List<EnhancedAdvancementWidget> selection = currentSelection();
        boolean isMulti = selection.size() > 1;
        int count = selection.size();

        Component resetLabel = isMulti
                ? Component.translatable("gui.reliable_advancements.context.reset_vanilla_multiple", count)
                : Component.translatable("gui.reliable_advancements.context.reset_vanilla");
        this.options.add(new ContextOption(resetLabel, true, () -> parentScreen.resetAdvancements(currentSelection())));

        Component deleteLabel = isMulti
                ? Component.translatable("gui.reliable_advancements.context.delete_multiple", count)
                : Component.translatable("gui.reliable_advancements.context.delete");
        this.options.add(new ContextOption(deleteLabel, true, () -> parentScreen.deleteAdvancements(currentSelection())));
    }

    private List<EnhancedAdvancementWidget> currentSelection() {
        if (EnhancedAdvancementsScreen.selectedWidgets.contains(widget) && EnhancedAdvancementsScreen.selectedWidgets.size() > 1) {
            return new ArrayList<>(EnhancedAdvancementsScreen.selectedWidgets);
        }
        return List.of(widget);
    }

    private void buildTabOptions(EnhancedAdvancementTab tab) {
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.edit_tab_properties"), false,
                () -> parentScreen.editTabProperties(tab)));
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.create_tab"), false,
                parentScreen::createNewTab));
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.restore_tab"), false, () -> {
            Minecraft.getInstance().setScreen(new RestoreTabScreen(parentScreen));
            parentScreen.closeContextMenu();
        }));
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.reset_tab"), true,
                () -> parentScreen.resetTabToVanilla(tab)));
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.delete_tab"), true,
                () -> parentScreen.deleteTab(tab)));
    }

    private void buildBackgroundOptions(int mouseX, int mouseY) {
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.create_advancement"), false, () -> parentScreen.createNewAdvancement(mouseX, mouseY)));
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.create_tab"), false, parentScreen::createNewTab));
        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.paste"), false, () -> {
            parentScreen.pasteAdvancement(mouseX, mouseY);
            parentScreen.closeContextMenu();
        }));

        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.restore_advancement"), false, () -> {
            Minecraft.getInstance().setScreen(new RestoreAdvancementScreen(parentScreen));
            parentScreen.closeContextMenu();
        }));

        this.options.add(new ContextOption(Component.translatable("gui.reliable_advancements.context.restore_tab"), false, () -> {
            Minecraft.getInstance().setScreen(new RestoreTabScreen(parentScreen));
            parentScreen.closeContextMenu();
        }));
    }

    private void openEditor(String tabName) {
        ResourceLocation id = widget.getAdvancement().holder().id();
        Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(id, tabName));
        parentScreen.closeContextMenu();
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 1000);

        guiGraphics.fillGradient(x, y, x + width, y + height, 0xF0101010, 0xF0101010);
        guiGraphics.renderOutline(x, y, width, height, 0xFF505050);

        Font font = Minecraft.getInstance().font;
        for (int i = 0; i < options.size(); i++) {
            ContextOption option = options.get(i);
            int optY = y + 2 + i * 20;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= optY && mouseY < optY + 20;

            if (hovered) {
                guiGraphics.fill(x + 1, optY, x + width - 1, optY + 20, option.isDestructive ? 0x80AA3333 : 0x80505050);
            }

            int textColor = hovered ? (option.isDestructive ? 0xFF5555 : 0xFFFFAA) : 0xFFFFFF;
            guiGraphics.drawString(font, option.label(), x + 6, optY + 6, textColor);
        }

        guiGraphics.pose().popPose();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            if (button == 0) {
                int index = (int) ((mouseY - y - 2) / 20);
                if (index >= 0 && index < options.size()) {
                    options.get(index).action.run();
                }
            }
            return true;
        }
        return false;
    }

    private record ContextOption(Component label, boolean isDestructive, Runnable action) {
    }
}
