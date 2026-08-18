package com.jvn.cirrus.client.compat.distanthorizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;

final class DistantHorizonsApiCompat {
    private static IDhApiConfig configs;
    private static IDhApiConfigValue<Boolean> cloudRendering;
    private static IDhApiConfigValue<Integer> chunkRenderDistance;
    private static Boolean lastCloudOverrideRequest;
    private static boolean overrideApplied;

    private DistantHorizonsApiCompat() {
    }

    static int beginFrame(
            boolean disableDistantClouds,
            boolean syncCloudDistance,
            int fallbackDistance
    ) {
        refreshConfigHandles();
        updateCloudOverride(disableDistantClouds);
        if (!syncCloudDistance || chunkRenderDistance == null) {
            return fallbackDistance;
        }

        Integer distance = chunkRenderDistance.getValue();
        return distance != null && distance > 0 ? distance : fallbackDistance;
    }

    private static void refreshConfigHandles() {
        IDhApiConfig currentConfigs = DhApi.Delayed.configs;
        if (currentConfigs == configs) {
            return;
        }

        if (overrideApplied && cloudRendering != null) {
            cloudRendering.clearValue();
        }
        configs = currentConfigs;
        cloudRendering = null;
        chunkRenderDistance = null;
        lastCloudOverrideRequest = null;
        overrideApplied = false;

        if (configs != null) {
            cloudRendering = configs.graphics().genericRendering().cloudRenderingEnabled();
            chunkRenderDistance = configs.graphics().chunkRenderDistance();
        }
    }

    private static void updateCloudOverride(boolean disableDistantClouds) {
        if (cloudRendering == null
                || Boolean.valueOf(disableDistantClouds).equals(lastCloudOverrideRequest)) {
            return;
        }

        if (overrideApplied) {
            cloudRendering.clearValue();
            overrideApplied = false;
        }

        if (disableDistantClouds && !Boolean.FALSE.equals(cloudRendering.getApiValue())) {
            overrideApplied = cloudRendering.setValue(false);
        }
        lastCloudOverrideRequest = disableDistantClouds;
    }
}
