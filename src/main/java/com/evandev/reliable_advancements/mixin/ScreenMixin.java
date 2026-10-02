package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//? if <26.1 {
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else {
/*import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

@Mixin(Screen.class)
public class ScreenMixin {
    @Unique
    private static final String reliable_advancements$OPEN_PREFIX = "/!open_advancement ";

    //? if <26.1 {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void interceptAdvancementClick(final Style style, final CallbackInfoReturnable<Boolean> cir) {
        if (style != null && style.getClickEvent() != null) {
            ClickEvent event = style.getClickEvent();

            if (event.getAction() == ClickEvent.Action.RUN_COMMAND && event.getValue().startsWith(reliable_advancements$OPEN_PREFIX)) {
                reliable_advancements$openAdvancement(event.getValue());
                cir.setReturnValue(true);
            }
        }
    }
    //?} else {
    /*@Inject(method = "clickCommandAction", at = @At("HEAD"), cancellable = true)
    private static void interceptAdvancementClick(final LocalPlayer player, final String command, final Screen screenAfterCommand, final CallbackInfo ci) {
        if (command != null && command.startsWith(reliable_advancements$OPEN_PREFIX)) {
            reliable_advancements$openAdvancement(command);
            ci.cancel();
        }
    }
    *///?}

    @Unique
    private static void reliable_advancements$openAdvancement(String command) {
        ResourceLocation id = ResourceLocation.tryParse(command.substring(reliable_advancements$OPEN_PREFIX.length()));
        Minecraft mc = Minecraft.getInstance();
        if (id != null && mc.player != null) {
            EnhancedAdvancementsScreen screen = new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements(), mc.screen);
            mc.setScreen(screen);
            screen.centerOnAdvancement(id);
        }
    }
}
