package com.evandev.advancement_enhancement.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementType.class)
public class AdvancementTypeMixin {
    @Inject(method = "createAnnouncement", at = @At("RETURN"), cancellable = true)
    private void addClickEventToAnnouncement(final AdvancementHolder advancementHolder, final ServerPlayer serverPlayer, final CallbackInfoReturnable<MutableComponent> cir) {
        MutableComponent original = cir.getReturnValue();
        if (original != null) {
            Style clickableStyle = original.getStyle().withClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/!open_advancement " + advancementHolder.id()
            ));

            cir.setReturnValue(original.copy().setStyle(clickableStyle));
        }
    }
}