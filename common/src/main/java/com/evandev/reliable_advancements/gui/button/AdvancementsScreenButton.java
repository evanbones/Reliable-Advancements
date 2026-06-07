package com.evandev.reliable_advancements.gui.button;

import com.evandev.reliable_advancements.config.InventoryButtonStyle;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.reference.Resources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AdvancementsScreenButton extends AbstractButton {

    private final Supplier<Integer> xSupplier;
    private final Supplier<Integer> ySupplier;

    public AdvancementsScreenButton(Supplier<Integer> xSupplier, Supplier<Integer> ySupplier, Component buttonText) {
        super(calculateX(xSupplier.get()), calculateY(ySupplier.get()), calculateWidth(), calculateHeight(), buttonText);
        this.xSupplier = xSupplier;
        this.ySupplier = ySupplier;
    }

    private static int calculateX(int x) {
        var config = ModConfig.get();
        return config.inventoryButtonStyle == InventoryButtonStyle.BUTTON ? x + config.inventoryButtonOffsetX : x - 28 + config.inventoryButtonOffsetX;
    }

    private static int calculateY(int y) {
        var config = ModConfig.get();
        return config.inventoryButtonStyle == InventoryButtonStyle.BUTTON ? y + config.inventoryButtonOffsetY : y - 28 + config.inventoryButtonOffsetY;
    }

    private static int calculateWidth() {
        return ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON ? 20 : 28;
    }

    private static int calculateHeight() {
        return ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON ? 18 : 32;
    }

    @Override
    public void extractContents(@NotNull GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        // Update positions/dimensions
        this.setX(calculateX(this.xSupplier.get()));
        this.setY(calculateY(this.ySupplier.get()));
        this.setWidth(calculateWidth());
        this.setHeight(calculateHeight());

        Minecraft mc = Minecraft.getInstance();
        this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();

        // Rendering logic remains consistent with the previous port
        if (ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON) {
            if (ModConfig.get().customInventoryButtonTexture == null || ModConfig.get().customInventoryButtonTexture.isEmpty()) {
                Identifier sprite = this.isHovered ? Identifier.withDefaultNamespace("widget/button_highlighted") : Identifier.withDefaultNamespace("widget/button");
                guiGraphicsExtractor.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            } else {
                String texToUse = (this.isHovered && ModConfig.get().customInventoryButtonTextureHovered != null && !ModConfig.get().customInventoryButtonTextureHovered.isEmpty()) ? ModConfig.get().customInventoryButtonTextureHovered : ModConfig.get().customInventoryButtonTexture;
                Identifier tex = Identifier.parse(texToUse);
                guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, tex, this.getX(), this.getY(), 0f, 0f, this.getWidth(), this.getHeight(), 256, 256);
            }

            if (ModConfig.get().customInventoryButtonIcon != null && !ModConfig.get().customInventoryButtonIcon.isEmpty()) {
                guiGraphicsExtractor.fakeItem(getIconStack(), this.getX() + 2, this.getY() + 1);
            }
        } else {
            guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.TABS, this.getX(), this.getY(), 56f, 0f, 28, 32, 256, 256);
            guiGraphicsExtractor.fakeItem(getIconStack(), this.getX() + 6, this.getY() + 10);
        }

        if (this.isHovered && ModConfig.get().enableButtonTooltip) {
            guiGraphicsExtractor.setTooltipForNextFrame(mc.font, Component.translatable("gui.advancements"), mouseX, mouseY);
        }
    }

    private ItemStack getIconStack() {
        String iconPath = ModConfig.get().customInventoryButtonIcon;
        if (iconPath == null || iconPath.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(iconPath))
                .map(holder -> new ItemStack(holder.asItem()))
                .orElse(new ItemStack(Items.BOOK));
    }

    @Override
    public void onPress(@NotNull InputWithModifiers input) {
        Minecraft.getInstance().setScreen(new EnhancedAdvancementsScreen(Minecraft.getInstance().player.connection.getAdvancements()));
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}