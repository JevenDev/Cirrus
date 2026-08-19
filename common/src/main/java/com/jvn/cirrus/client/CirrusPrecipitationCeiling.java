package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class CirrusPrecipitationCeiling {
    private static final double CLOUD_RENDER_OFFSET = 0.33D;

    private CirrusPrecipitationCeiling() {
    }

    public static double absolute(ClientLevel level) {
        if (level == null
                || !CirrusCloudMode.isActive(Minecraft.getInstance().options.getCloudsType())) {
            return Double.POSITIVE_INFINITY;
        }

        float cloudHeight = level.effects().getCloudHeight();
        if (Float.isNaN(cloudHeight)) {
            return Double.POSITIVE_INFINITY;
        }

        double highestLayer = cloudHeight + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get();
        if (CirrusConfig.TOP_LAYER_ENABLED.get()) {
            highestLayer += CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get()
                    + CirrusConfig.TOP_LAYER_HEIGHT_OFFSET.get();
        } else if (CirrusConfig.UPPER_LAYER_ENABLED.get()) {
            highestLayer += CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get();
        }
        return highestLayer + CLOUD_RENDER_OFFSET;
    }

    public static float relative(ClientLevel level, double cameraY) {
        return (float)(absolute(level) - cameraY);
    }
}
