package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;

public final class CirrusSky {
    private static final float CLOUD_CLIP_MARGIN = 96.0F;

    private CirrusSky() {
    }

    public static float minimumFarPlane() {
        return CirrusConfig.CLOUD_RENDER_DISTANCE.get() * 16.0F + CLOUD_CLIP_MARGIN;
    }
}
