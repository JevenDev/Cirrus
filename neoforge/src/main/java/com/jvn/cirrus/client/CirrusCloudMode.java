package com.jvn.cirrus.client;

import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.CloudStatus;

public final class CirrusCloudMode {
    private CirrusCloudMode() {
    }

    public static boolean isActive(CloudStatus cloudStatus) {
        return CirrusConfig.CUSTOM_CLOUDS_ENABLED.get()
                && (cloudStatus != CloudStatus.OFF
                || DistantHorizonsCompat.shouldPrioritizeCirrusClouds());
    }
}
