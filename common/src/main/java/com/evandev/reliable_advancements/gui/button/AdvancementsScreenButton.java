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
    protected void extractContents(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        this.setX(calculateX(this.xSupplier.get()));
        this.setY(calculateY(this.ySupplier.get()));
        this.setWidth(calculateWidth());
        this.setHeight(calculateHeight());

        Minecraft mc = Minecraft.getInstance();
        this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight();

        if (ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON) {
            if (ModConfig.get().customInventoryButtonTexture == null || ModConfig.get().customInventoryButtonTexture.isEmpty()) {
                Identifier sprite = this.isHovered ? Identifier.withDefaultNamespace("widget/button_highlighted") : Identifier.withDefaultNamespace("widget/button");
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight());
            } else {
                String texToUse = (this.isHovered && ModConfig.get().customInventoryButtonTextureHovered != null && !ModConfig.get().customInventoryButtonTextureHovered.isEmpty()) ? ModConfig.get().customInventoryButtonTextureHovered : ModConfig.get().customInventoryButtonTexture;
                Identifier tex = Identifier.parse(texToUse);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tex, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            }

            if (ModConfig.get().customInventoryButtonIcon != null && !ModConfig.get().customInventoryButtonIcon.isEmpty()) {
                guiGraphics.fakeItem(getIconStack(), this.getX() + 2, this.getY() + 1);
            }
        } else {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.TABS, this.getX(), this.getY(), 56, 0, 28, 32, 256, 256);
            guiGraphics.fakeItem(getIconStack(), this.getX() + 6, this.getY() + 10);
        }

        if (this.isHovered && ModConfig.get().enableButtonTooltip) {
            guiGraphics.setTooltipForNextFrame(mc.font, Component.translatable("gui.advancements"), mouseX, mouseY);
        }
    }

    private ItemStack getIconStack() {
        if (ModConfig.get().customInventoryButtonIcon == null || ModConfig.get().customInventoryButtonIcon.isEmpty())
            return ItemStack.EMPTY;
        try {
            return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(ModConfig.get().customInventoryButtonIcon)));
        } catch (Exception e) {
            return new ItemStack(Items.BOOK);
        }
    }

    @Override
    public void onPress(InputWithModifiers input) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreenAndShow(new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements(), mc.gui.screen()));
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }
}
