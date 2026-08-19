package com.jvn.cirrus.client.util;

import net.minecraft.util.Mth;

public final class CirrusEasing {
    private CirrusEasing() {
    }

    public static float smoothstep(float progress) {
        float value = Mth.clamp(progress, 0.0F, 1.0F);
        return value * value * (3.0F - 2.0F * value);
    }

    public static double smootherstep(double progress) {
        double value = Mth.clamp(progress, 0.0D, 1.0D);
        return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
    }
}
