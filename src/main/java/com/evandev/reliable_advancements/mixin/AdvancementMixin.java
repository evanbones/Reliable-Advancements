package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=26.3 {
/*import com.evandev.reliable_advancements.advancements.DisplayCompat;
import com.mojang.serialization.DataResult;
import net.minecraft.network.codec.StreamCodec;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
*///?}

@Mixin(Advancement.class)
public abstract class AdvancementMixin implements IMultiParentAdvancement {

    @Unique
    private List<ResourceLocation> reliable_advancements$parents = null;

    //? if <26.3 {
    @Inject(method = "read", at = @At("RETURN"))
    private static void reliable_advancements$readExtraParents(RegistryFriendlyByteBuf buffer, CallbackInfoReturnable<Advancement> cir) {
        reliable_advancements$readParents(buffer, cir.getReturnValue());
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void reliable_advancements$writeExtraParents(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        reliable_advancements$writeParents(buffer, (Advancement) (Object) this);
    }
    //?} else {
    /*@Shadow
    @Final
    @Mutable
    public static StreamCodec<RegistryFriendlyByteBuf, Advancement> STREAM_CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void reliable_advancements$wrapStreamCodec(CallbackInfo ci) {
        StreamCodec<RegistryFriendlyByteBuf, Advancement> base = STREAM_CODEC;
        STREAM_CODEC = StreamCodec.of(
                (buffer, advancement) -> {
                    base.encode(buffer, advancement);
                    reliable_advancements$writeParents(buffer, advancement);
                },
                buffer -> {
                    Advancement advancement = base.decode(buffer);
                    reliable_advancements$readParents(buffer, advancement);
                    return advancement;
                }
        );
    }

    @Inject(method = "validate(Lnet/minecraft/advancements/Advancement;)Lcom/mojang/serialization/DataResult;", at = @At("HEAD"), cancellable = true)
    private static void reliable_advancements$allowBackgroundlessRoots(Advancement advancement, CallbackInfoReturnable<DataResult<Advancement>> cir) {
        if (advancement.parent().isEmpty() && advancement.display().isPresent() && DisplayCompat.background(advancement.display().get()).isEmpty()) {
            cir.setReturnValue(advancement.requirements().validate(advancement.criteria().keySet()).map(r -> advancement));
        }
    }
    *///?}

    @Unique
    private static void reliable_advancements$readParents(RegistryFriendlyByteBuf buffer, Advancement advancement) {
        if (advancement != null) {
            boolean hasExplicitParents = buffer.readBoolean();
            if (hasExplicitParents) {
                int count = buffer.readVarInt();
                List<ResourceLocation> parents = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    parents.add(buffer.readResourceLocation());
                }
                if (parents.isEmpty() && advancement.parent().isPresent()) {
                    parents.add(advancement.parent().get());
                }
                IMultiParentAdvancement.setParents(advancement, parents);
            } else if (advancement.parent().isPresent()) {
                IMultiParentAdvancement.setParents(advancement, List.of(advancement.parent().get()));
            }
        }
    }

    @Unique
    private static void reliable_advancements$writeParents(RegistryFriendlyByteBuf buffer, Advancement advancement) {
        List<ResourceLocation> parents = IMultiParentAdvancement.getParents(advancement);
        boolean hasExplicitParents = parents != null && !parents.isEmpty();
        buffer.writeBoolean(hasExplicitParents);
        if (hasExplicitParents) {
            buffer.writeVarInt(parents.size());
            for (ResourceLocation parent : parents) {
                buffer.writeResourceLocation(parent);
            }
        }
    }

    @Override
    public List<ResourceLocation> reliable_advancements$getParents() {
        if (this.reliable_advancements$parents == null || this.reliable_advancements$parents.isEmpty()) {
            Advancement self = (Advancement) (Object) this;
            return self.parent().map(List::of).orElse(List.of());
        }
        return this.reliable_advancements$parents;
    }

    @Override
    public void reliable_advancements$setParents(List<ResourceLocation> parents) {
        if (parents == null || parents.isEmpty()) {
            Advancement self = (Advancement) (Object) this;
            if (self.parent().isPresent()) {
                this.reliable_advancements$parents = new ArrayList<>(List.of(self.parent().get()));
                return;
            }
            this.reliable_advancements$parents = new ArrayList<>();
            return;
        }
        this.reliable_advancements$parents = new ArrayList<>(parents);
    }
}
