package com.evandev.reliable_advancements.advancements;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

//? if >=26.1 {
/*import net.minecraft.core.ClientAsset;
*///?}

public final class DisplayCompat {
    private DisplayCompat() {
    }

    public static Component title(DisplayInfo display) {
        //? if <26.3 {
        return display.getTitle();
        //?} else {
        /*return display.title();
        *///?}
    }

    public static Component description(DisplayInfo display) {
        //? if <26.3 {
        return display.getDescription();
        //?} else {
        /*return display.description();
        *///?}
    }

    public static AdvancementType type(DisplayInfo display) {
        //? if <26.3 {
        return display.getType();
        //?} else {
        /*return display.type();
        *///?}
    }

    public static boolean hidden(DisplayInfo display) {
        //? if <26.3 {
        return display.isHidden();
        //?} else {
        /*return display.hidden();
        *///?}
    }

    public static ItemStack icon(DisplayInfo display) {
        //? if <26.1 {
        return display.getIcon();
        //?} else if <26.3 {
        /*return display.getIcon().create();
        *///?} else {
        /*return display.icon().create();
        *///?}
    }

    public static Item iconItem(DisplayInfo display) {
        //? if <26.1 {
        return display.getIcon().getItem();
        //?} else if <26.3 {
        /*return display.getIcon().item().value();
        *///?} else {
        /*return display.icon().item().value();
        *///?}
    }

    /**
     * The background texture path (e.g. {@code minecraft:textures/gui/advancements/backgrounds/stone.png}).
     */
    public static Optional<ResourceLocation> background(DisplayInfo display) {
        //? if <26.1 {
        return display.getBackground();
        //?} else if <26.3 {
        /*return display.getBackground().map(ClientAsset.ResourceTexture::texturePath);
        *///?} else {
        /*return display.background().map(ClientAsset.ResourceTexture::texturePath);
        *///?}
    }

    public static float x(AdvancementNode node) {
        //? if <26.3 {
        return node.advancement().display().map(DisplayInfo::getX).orElse(0.0F);
        //?} else {
        /*return node.x();
        *///?}
    }

    public static float y(AdvancementNode node) {
        //? if <26.3 {
        return node.advancement().display().map(DisplayInfo::getY).orElse(0.0F);
        //?} else {
        /*return node.y();
        *///?}
    }
}
