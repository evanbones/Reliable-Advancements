package com.evandev.reliable_advancements.mixin;

import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Advancement.class)
public class AdvancementMixin {
    @Inject(method = "getChatComponent", at = @At("RETURN"), cancellable = true)
    private void addClickEventToChatComponent(final CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (original != null) {
            Advancement advancement = (Advancement) (Object) this;
            Style clickableStyle = original.getStyle().withClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/!open_advancement " + advancement.getId()
            ));

            cir.setReturnValue(original.copy().setStyle(clickableStyle));
        }
    }
}
