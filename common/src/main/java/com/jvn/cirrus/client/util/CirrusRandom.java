package com.jvn.cirrus.client.util;

import net.minecraft.util.RandomSource;

import java.util.random.RandomGenerator;

public final class CirrusRandom {
    private CirrusRandom() {
    }

    public static float signedFloat(RandomSource random) {
        return random.nextFloat() * 2.0F - 1.0F;
    }

    public static double signedDouble(RandomSource random) {
        return random.nextDouble() * 2.0D - 1.0D;
    }

    public static float signedFloat(RandomGenerator random) {
        return random.nextFloat() * 2.0F - 1.0F;
    }

    public static double signedDouble(RandomGenerator random) {
        return random.nextDouble() * 2.0D - 1.0D;
    }
}
