package com.evandev.reliable_advancements.advancements;

import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class AdvancementDisplayInfoRegistry {
    private final Map<ResourceLocation, AdvancementDisplayInfo> registry;

    public AdvancementDisplayInfoRegistry(AdvancementNode advancementNode) {
        registry = new HashMap<>();
    }

    public AdvancementDisplayInfo get(AdvancementHolder advancementHolder) {
        return registry.getOrDefault(advancementHolder.id(), new AdvancementDisplayInfo(advancementHolder));
    }

    private void load(ResourceLocation location, ServerLevel serverLevel) {
        Services.PLATFORM.getAdvancementVisitor().findAdvancements(location, serverLevel, null,
                (root, file) ->
                {
                    String relative;
                    try {
                        relative = root.relativize(file).toString();
                    } catch (Exception e) {
                        relative = "";
                    }
                    if (!"json".equals(FilenameUtils.getExtension(file.toString())) || relative.startsWith("_"))
                        return true;

                    String name = FilenameUtils.removeExtension(relative).replaceAll("\\\\", "/");
                    ResourceLocation key = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), name);

                    if (!registry.containsKey(key)) {
                        BufferedReader reader = null;

                        try {
                            reader = Files.newBufferedReader(file);
                            JsonObject advancement = JsonParser.parseReader(reader).getAsJsonObject();
                            JsonObject betterDisplay = advancement.getAsJsonObject("better_display");
                            registry.put(key, new AdvancementDisplayInfo(key, betterDisplay));
                        } catch (JsonParseException e) {
                            Constants.LOG.error("Parsing error loading built-in advancement {}", key, e);
                            return false;
                        } catch (IOException e) {
                            Constants.LOG.error("Couldn't read advancement {} from {}", key, file, e);
                            return false;
                        } finally {
                            IOUtils.closeQuietly(reader);
                        }
                    }

                    return true;
                }, true, true);
    }
}
