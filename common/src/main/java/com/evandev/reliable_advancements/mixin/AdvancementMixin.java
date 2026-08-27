package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Advancement.class)
public abstract class AdvancementMixin implements IMultiParentAdvancement {

    @Unique
    private List<ResourceLocation> reliable_advancements$parents = null;

    @Inject(method = "read", at = @At("RETURN"))
    private static void reliable_advancements$readExtraParents(RegistryFriendlyByteBuf buffer, CallbackInfoReturnable<Advancement> cir) {
        Advancement advancement = cir.getReturnValue();
        if (advancement != null) {
            boolean hasExplicitParents = buffer.readBoolean();
            if (hasExplicitParents) {
                List<ResourceLocation> parents = buffer.readCollection(ArrayList::new, FriendlyByteBuf::readResourceLocation);
                IMultiParentAdvancement.setParents(advancement, parents);
            }
        }
    }

    @Override
    public List<ResourceLocation> reliable_advancements$getParents() {
        if (this.reliable_advancements$parents == null) {
            Advancement self = (Advancement) (Object) this;
            return self.parent().map(List::of).orElse(List.of());
        }
        return this.reliable_advancements$parents;
    }

    @Override
    public void reliable_advancements$setParents(List<ResourceLocation> parents) {
        this.reliable_advancements$parents = parents != null ? new ArrayList<>(parents) : new ArrayList<>();
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void reliable_advancements$writeExtraParents(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        boolean hasExplicitParents = this.reliable_advancements$parents != null;
        buffer.writeBoolean(hasExplicitParents);
        if (hasExplicitParents) {
            buffer.writeCollection(this.reliable_advancements$parents, FriendlyByteBuf::writeResourceLocation);
        }
    }
}
