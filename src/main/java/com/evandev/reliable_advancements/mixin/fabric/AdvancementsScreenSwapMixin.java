package com.evandev.reliable_advancements.mixin.fabric;

//? if fabric {
/*import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Swaps the vanilla advancements screen for ours whenever it is opened.
//? if <26.2 {
@Mixin(Minecraft.class)
//?} else {
/^@Mixin(Gui.class)
^///?}
public class AdvancementsScreenSwapMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private Screen swapAdvancementsScreen(Screen screen) {
        if (screen instanceof AdvancementsScreen) {
            Minecraft mc = Minecraft.getInstance();
            return new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements(), mc.screen);
        } else {
            return screen;
        }
    }
}
*///?}
