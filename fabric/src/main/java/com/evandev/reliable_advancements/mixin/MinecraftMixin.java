package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @ModifyVariable(method = "setScreenAndShow", at = @At("HEAD"), argsOnly = true, name = "screen")
    private Screen swapAdvancementsScreen(Screen screen) {
        if (screen instanceof AdvancementsScreen) {
            Minecraft mc = Minecraft.getInstance();
            return new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements());
        } else {
            return screen;
        }
    }
}
