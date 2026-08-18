package com.jvn.cirrus.client.compat.distanthorizons;

import com.jvn.cirrus.config.CirrusConfig;
import net.neoforged.fml.ModList;

public final class DistantHorizonsCompat {
    private static final String MOD_ID = "distanthorizons";

    private DistantHorizonsCompat() {
    }

    public static boolean shouldPrioritizeCirrusClouds() {
        return isLoaded()
                && CirrusConfig.CUSTOM_CLOUDS_ENABLED.get()
                && CirrusConfig.DISTANT_HORIZONS_COMPATIBILITY.get();
    }

    public static int cloudRenderDistanceChunks() {
        int configuredDistance = CirrusConfig.CLOUD_RENDER_DISTANCE.get();
        if (!isLoaded()
                || !CirrusConfig.DISTANT_HORIZONS_COMPATIBILITY.get()
                || !CirrusConfig.SYNC_CLOUD_DISTANCE_WITH_DISTANT_HORIZONS.get()) {
            return configuredDistance;
        }
        return DistantHorizonsApiCompat.cloudRenderDistanceChunks(configuredDistance);
    }

    public static void updateCloudOverride() {
        if (isLoaded()) {
            DistantHorizonsApiCompat.updateCloudOverride(shouldPrioritizeCirrusClouds());
        }
    }

    private static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }
}
