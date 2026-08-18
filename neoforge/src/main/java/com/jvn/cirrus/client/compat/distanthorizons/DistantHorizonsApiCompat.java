package com.jvn.cirrus.client.compat.distanthorizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;

final class DistantHorizonsApiCompat {
    private static IDhApiConfigValue<Boolean> cloudRendering;
    private static boolean overrideApplied;

    private DistantHorizonsApiCompat() {
    }

    static void updateCloudOverride(boolean disableDistantClouds) {
        IDhApiConfig configs = DhApi.Delayed.configs;
        if (configs == null) {
            return;
        }

        IDhApiConfigValue<Boolean> currentCloudRendering =
                configs.graphics().genericRendering().cloudRenderingEnabled();
        if (currentCloudRendering != cloudRendering) {
            cloudRendering = currentCloudRendering;
            overrideApplied = false;
        }

        if (disableDistantClouds) {
            if (!Boolean.FALSE.equals(cloudRendering.getApiValue())) {
                overrideApplied = cloudRendering.setValue(false);
            }
        } else if (overrideApplied) {
            cloudRendering.clearValue();
            overrideApplied = false;
        }
    }

    static int cloudRenderDistanceChunks(int fallbackDistance) {
        IDhApiConfig configs = DhApi.Delayed.configs;
        if (configs == null) {
            return fallbackDistance;
        }

        Integer distance = configs.graphics().chunkRenderDistance().getValue();
        if (distance == null || distance <= 0) {
            return fallbackDistance;
        }

        return distance;
    }
}
