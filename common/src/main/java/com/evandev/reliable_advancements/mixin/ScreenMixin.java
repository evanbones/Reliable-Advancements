package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void onSendText(final Style clicked, boolean allowInsertions, final CallbackInfoReturnable<Boolean> cir) {
        if (clicked != null && clicked.getClickEvent() != null) {
            ClickEvent event = clicked.getClickEvent();

            if (event.action() == ClickEvent.Action.RUN_COMMAND && event instanceof ClickEvent.RunCommand(
                    String command
            )) {
                if (command.startsWith("/!open_advancement ")) {
                    String idStr = command.substring("/!open_advancement ".length());
                    Identifier id = Identifier.tryParse(idStr);

                    if (id != null) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            EnhancedAdvancementsScreen screen = new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements());
                            mc.setScreenAndShow(screen);
                            screen.centerOnAdvancement(id);
                        }
                    }
                    cir.setReturnValue(true);
                }
            }
        }
    }
}