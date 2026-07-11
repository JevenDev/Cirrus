package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;

public final class CirrusSky {
    private static final float CLOUD_CLIP_MARGIN = 96.0F;

    private CirrusSky() {
    }

    public static float minimumFarPlane() {
        float cloudDistance = CirrusConfig.CLOUD_RENDER_DISTANCE.get() * 16.0F;
        float lowerOffset = Math.abs(CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get().floatValue());
        float upperOffset = CirrusConfig.UPPER_LAYER_ENABLED.get()
                ? CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get().floatValue()
                : 0.0F;
        float verticalAllowance = 256.0F + lowerOffset + upperOffset;
        return (float)Math.hypot(cloudDistance, verticalAllowance) + CLOUD_CLIP_MARGIN;
    }
}
