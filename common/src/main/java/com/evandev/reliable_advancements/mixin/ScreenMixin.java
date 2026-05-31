package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void interceptAdvancementClick(final Style style, final CallbackInfoReturnable<Boolean> cir) {
        if (style != null && style.getClickEvent() != null) {
            ClickEvent event = style.getClickEvent();

            if (event.getAction() == ClickEvent.Action.RUN_COMMAND && event.getValue().startsWith("/!open_advancement ")) {
                String idStr = event.getValue().substring("/!open_advancement ".length());
                ResourceLocation id = ResourceLocation.tryParse(idStr);

                if (id != null) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player != null) {
                        EnhancedAdvancementsScreen screen = new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements());
                        mc.setScreen(screen);

                        screen.centerOnAdvancement(id);
                    }
                }

                cir.setReturnValue(true);
            }
        }
    }
}