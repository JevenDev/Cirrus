package com.jvn.cirrus.config;

import net.minecraft.client.CloudStatus;

public enum CloudQuality {
    FAST(CloudStatus.FAST),
    FANCY(CloudStatus.FANCY);

    private final CloudStatus cloudStatus;

    CloudQuality(CloudStatus cloudStatus) {
        this.cloudStatus = cloudStatus;
    }

    public CloudStatus cloudStatus() {
        return cloudStatus;
    }
}
