package com.jvn.cirrus.client;

import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import com.jvn.cirrus.config.CirrusConfig;

public final class CirrusSky {
    private static final float CLOUD_CLIP_MARGIN = 96.0F;

    private CirrusSky() {
    }

    public static float minimumFarPlane(boolean cirrusCloudsActive) {
        if (!cirrusCloudsActive) {
            return 0.0F;
        }

        float cloudDistance = DistantHorizonsCompat.cloudRenderDistanceChunks() * 16.0F;
        float lowerOffset = Math.abs(CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get().floatValue());
        boolean topEnabled = CirrusConfig.TOP_LAYER_ENABLED.get();
        float upperOffset = CirrusConfig.UPPER_LAYER_ENABLED.get() || topEnabled
                ? CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get().floatValue()
                : 0.0F;
        float topOffset = topEnabled
                ? CirrusConfig.TOP_LAYER_HEIGHT_OFFSET.get().floatValue()
                : 0.0F;
        float verticalAllowance = 256.0F + lowerOffset + upperOffset + topOffset;
        return (float)Math.hypot(cloudDistance, verticalAllowance) + CLOUD_CLIP_MARGIN;
    }
}
