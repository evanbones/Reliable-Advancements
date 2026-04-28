package com.evandev.advancement_enhancement.platform;

import com.evandev.advancement_enhancement.platform.services.IAdvancementVisitor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FabricAdvancementVisitor implements IAdvancementVisitor {
    @Override
    public boolean findAdvancements(ResourceLocation location, ServerLevel serverLevel, Function<Path, Boolean> preprocessor, BiFunction<Path, Path, Boolean> processor, boolean defaultUnfoundRoot, boolean visitAllFiles) {
        return false;
    }
}
