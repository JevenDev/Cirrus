package com.jvn.cirrus.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.cirrus.client.util.CirrusEasing;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CirrusAuroraBiomes {
    private static final String DIRECTORY = "cirrus/aurora_biomes";
    private static Map<Identifier, Boolean> rules = Map.of();

    private CirrusAuroraBiomes() {
    }

    public static void reload(ResourceManager resourceManager) {
        Map<Identifier, Boolean> loaded = new HashMap<>();
        resourceManager.listResources(DIRECTORY, id -> id.getPath().endsWith(".json"))
                .forEach((id, resource) -> {
                    String path = id.getPath();
                    Identifier biome = Identifier.fromNamespaceAndPath(
                            id.getNamespace(), path.substring(DIRECTORY.length() + 1, path.length() - 5)
                    );
                    try (Reader reader = resource.openAsReader()) {
                        loaded.put(biome, parse(JsonParser.parseReader(reader)));
                    } catch (IOException | IllegalArgumentException | JsonParseException exception) {
                        Cirrus.LOGGER.warn("Unable to load Cirrus aurora biome rule {} from {}",
                                id, resource.sourcePackId(), exception);
                    }
                });
        rules = Map.copyOf(loaded);
    }

    static boolean parse(JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Aurora biome rule must be an object");
        }
        JsonElement enabled = element.getAsJsonObject().get("enabled");
        if (enabled == null || !enabled.isJsonPrimitive() || !enabled.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Aurora biome rule requires a boolean enabled value");
        }
        return enabled.getAsBoolean();
    }

    public static Set<Identifier> biomes() {
        return rules.keySet();
    }

    public static Selection selection(Identifier biome) {
        Boolean enabled = CirrusConfig.AURORA_BIOMES.get().get(biome.toString());
        return enabled == null ? Selection.DEFAULT : enabled ? Selection.ENABLED : Selection.DISABLED;
    }

    public static void setSelection(Identifier biome, Selection selection) {
        Map<String, Boolean> overrides = new HashMap<>(CirrusConfig.AURORA_BIOMES.get());
        if (selection == Selection.DEFAULT) {
            overrides.remove(biome.toString());
        } else {
            overrides.put(biome.toString(), selection == Selection.ENABLED);
        }
        CirrusConfig.AURORA_BIOMES.set(overrides);
    }

    public static float strength(Holder<Biome> biome) {
        Identifier id = biome.unwrapKey().map(key -> key.identifier()).orElse(null);
        if (id != null) {
            Boolean enabled = CirrusConfig.AURORA_BIOMES.get().get(id.toString());
            if (enabled == null) {
                enabled = rules.get(id);
            }
            if (enabled != null) {
                return enabled ? 1.0F : 0.0F;
            }
        }
        if (!CirrusConfig.AURORA_UNLISTED_BIOMES.get()) {
            return 0.0F;
        }
        if (!CirrusConfig.AURORA_COLD_BIOMES_ONLY.get()) {
            return 1.0F;
        }
        float cold = Mth.clamp((0.15F - biome.value().getBaseTemperature()) / 0.15F, 0.0F, 1.0F);
        return CirrusEasing.smoothstep(cold);
    }

    public enum Selection {
        DEFAULT,
        ENABLED,
        DISABLED
    }
}
