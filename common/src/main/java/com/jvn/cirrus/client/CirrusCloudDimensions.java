package com.jvn.cirrus.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CirrusCloudDimensions {
    private static final String DIRECTORY = "cirrus/cloud_dimensions";
    private static final float FALLBACK_HEIGHT = 192.0F;
    private static Map<ResourceLocation, Rule> rules = Map.of();

    private CirrusCloudDimensions() {
    }

    public static void reload(ResourceManager resourceManager) {
        Map<ResourceLocation, Rule> loaded = new HashMap<>();
        resourceManager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    String path = id.getPath();
                    ResourceLocation dimension = ResourceLocation.fromNamespaceAndPath(
                            id.getNamespace(), path.substring(DIRECTORY.length() + 1, path.length() - 5)
                    );
                    try (Reader reader = resource.openAsReader()) {
                        loaded.put(dimension, Rule.parse(JsonParser.parseReader(reader)));
                    } catch (IOException | IllegalArgumentException | JsonParseException exception) {
                        Cirrus.LOGGER.warn("Unable to load Cirrus cloud dimension rule {} from {}",
                                id, resource.sourcePackId(), exception);
                    }
                });
        rules = Map.copyOf(loaded);
    }

    public static Set<ResourceLocation> dimensions() {
        return rules.keySet();
    }

    public static boolean defaultEnabled(ResourceLocation dimension) {
        Rule rule = rules.get(dimension);
        return rule != null && rule.enabled();
    }

    public static boolean enabled(ResourceLocation dimension) {
        return CirrusConfig.CLOUD_DIMENSIONS.get().getOrDefault(dimension.toString(), defaultEnabled(dimension));
    }

    public static void setEnabled(ResourceLocation dimension, boolean enabled) {
        Map<String, Boolean> overrides = new HashMap<>(CirrusConfig.CLOUD_DIMENSIONS.get());
        if (enabled == defaultEnabled(dimension)) {
            overrides.remove(dimension.toString());
        } else {
            overrides.put(dimension.toString(), enabled);
        }
        CirrusConfig.CLOUD_DIMENSIONS.set(overrides);
        CirrusCloudAttachment.invalidate();
    }

    public static float cloudHeight(ClientLevel level) {
        if (level == null || !enabled(level.dimension().location())) {
            return Float.NaN;
        }
        Rule rule = rules.get(level.dimension().location());
        float height = rule != null && rule.cloudHeight() != null
                ? rule.cloudHeight() : level.effects().getCloudHeight();
        return Float.isFinite(height) ? height : FALLBACK_HEIGHT;
    }

    public record Rule(boolean enabled, Float cloudHeight) {
        public static Rule parse(JsonElement element) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Cloud dimension rule must be an object");
            }
            JsonObject object = element.getAsJsonObject();
            JsonElement enabled = object.get("enabled");
            if (enabled == null || !enabled.isJsonPrimitive() || !enabled.getAsJsonPrimitive().isBoolean()) {
                throw new IllegalArgumentException("Cloud dimension rule requires a boolean enabled value");
            }
            Float height = null;
            if (object.has("cloud_height")) {
                JsonElement value = object.get("cloud_height");
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                    throw new IllegalArgumentException("Cloud height must be a finite number");
                }
                height = value.getAsFloat();
                if (!Float.isFinite(height)) {
                    throw new IllegalArgumentException("Cloud height must be a finite number");
                }
            }
            return new Rule(enabled.getAsBoolean(), height);
        }
    }
}
