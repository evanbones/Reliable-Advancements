package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "clickCommandAction", at = @At("HEAD"), cancellable = true)
    private static void interceptAdvancementClick(final LocalPlayer player, final String command, final Screen screenAfterCommand, final CallbackInfo ci) {
        if (command != null && command.startsWith("/!open_advancement ")) {
            String idStr = command.substring("/!open_advancement ".length());
            Identifier id = Identifier.tryParse(idStr);

            if (id != null) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    EnhancedAdvancementsScreen screen = new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements(), mc.gui.screen());
                    mc.setScreenAndShow(screen);

                    screen.centerOnAdvancement(id);
                }
            }

            ci.cancel();
        }
    }
}