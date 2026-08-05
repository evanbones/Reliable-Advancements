package com.evandev.reliable_advancements.gui.button;

import com.evandev.reliable_advancements.config.InventoryButtonStyle;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.reference.Resources;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class AdvancementsScreenButton extends AbstractButton {

    private final Supplier<Integer> xSupplier;
    private final Supplier<Integer> ySupplier;

    public AdvancementsScreenButton(Supplier<Integer> xSupplier, Supplier<Integer> ySupplier, Component buttonText) {
        super(
                calculateX(xSupplier.get()),
                calculateY(ySupplier.get()),
                calculateWidth(),
                calculateHeight(),
                buttonText
        );
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
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        this.setX(calculateX(this.xSupplier.get()));
        this.setY(calculateY(this.ySupplier.get()));
        this.setWidth(calculateWidth());
        this.setHeight(calculateHeight());

        Minecraft mc = Minecraft.getInstance();
        this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();

        if (ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON) {
            if (ModConfig.get().customInventoryButtonTexture == null || ModConfig.get().customInventoryButtonTexture.isEmpty()) {
                ResourceLocation sprite = this.isHovered ? ResourceLocation.withDefaultNamespace("widget/button_highlighted") : ResourceLocation.withDefaultNamespace("widget/button");
                guiGraphics.blitSprite(sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            } else {
                String texToUse = (this.isHovered && ModConfig.get().customInventoryButtonTextureHovered != null && !ModConfig.get().customInventoryButtonTextureHovered.isEmpty()) ? ModConfig.get().customInventoryButtonTextureHovered : ModConfig.get().customInventoryButtonTexture;
                ResourceLocation tex = ResourceLocation.parse(texToUse);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
                RenderSystem.disableBlend();
            }

            if (ModConfig.get().customInventoryButtonIcon != null && !ModConfig.get().customInventoryButtonIcon.isEmpty()) {
                guiGraphics.renderFakeItem(getIconStack(), this.getX() + 2, this.getY() + 1);
            }
        } else {
            guiGraphics.blit(Resources.Gui.TABS, this.getX(), this.getY(), 56, 0, 28, 32);
            RenderSystem.defaultBlendFunc();
            guiGraphics.renderFakeItem(getIconStack(), this.getX() + 6, this.getY() + 10);
        }

        if (this.isHovered && ModConfig.get().enableButtonTooltip) {
            guiGraphics.renderTooltip(mc.font, Component.translatable("gui.advancements"), mouseX, mouseY);
        }
    }

    private ItemStack getIconStack() {
        if (ModConfig.get().customInventoryButtonIcon == null || ModConfig.get().customInventoryButtonIcon.isEmpty())
            return ItemStack.EMPTY;
        try {
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(ModConfig.get().customInventoryButtonIcon)));
        } catch (Exception e) {
            return new ItemStack(Items.BOOK);
        }
    }

    @Override
    public void onPress() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements(), mc.screen));
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }
}
