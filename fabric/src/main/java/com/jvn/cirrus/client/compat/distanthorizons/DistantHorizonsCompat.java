package com.jvn.cirrus.client.compat.distanthorizons;

import com.jvn.cirrus.config.CirrusConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.util.function.BiConsumer;

public final class DistantHorizonsCompat {
    private static final String MOD_ID = "distanthorizons";
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);

    private static boolean initialized;
    private static boolean prioritizeCirrusClouds;
    private static int effectiveCloudRenderDistance =
            CirrusConfig.CLOUD_RENDER_DISTANCE_SETTING.defaultValue();

    private DistantHorizonsCompat() {
    }

    public static boolean shouldPrioritizeCirrusClouds() {
        ensureInitialized();
        return prioritizeCirrusClouds;
    }

    public static int cloudRenderDistanceChunks() {
        ensureInitialized();
        return effectiveCloudRenderDistance;
    }

    public static void beginFrame() {
        initialized = true;
        int configuredDistance = CirrusConfig.CLOUD_RENDER_DISTANCE.get();
        boolean compatibilityEnabled =
                LOADED && CirrusConfig.DISTANT_HORIZONS_COMPATIBILITY.get();
        prioritizeCirrusClouds =
                compatibilityEnabled && CirrusConfig.CUSTOM_CLOUDS_ENABLED.get();
        if (!LOADED) {
            effectiveCloudRenderDistance = configuredDistance;
            return;
        }

        boolean syncDistance = compatibilityEnabled
                && CirrusConfig.SYNC_CLOUD_DISTANCE_WITH_DISTANT_HORIZONS.get();
        effectiveCloudRenderDistance = DistantHorizonsApiCompat.beginFrame(
                prioritizeCirrusClouds,
                syncDistance,
                configuredDistance
        );
    }

    public static void setBeforeApplyShaderCallback(BiConsumer<float[], float[]> callback) {
        if (LOADED) {
            DistantHorizonsApiCompat.setBeforeApplyShaderCallback(callback);
        }
    }

    private static void ensureInitialized() {
        if (!initialized) {
            beginFrame();
        }
    }
}
