package com.jvn.cirrus.client.compat.fog;

import dev.architectury.platform.Platform;

public final class FogModCompat {
    private static final boolean BETTER_FOG_LOADED = Platform.isModLoaded("betterfog");
    private static final boolean EXTERNAL_FOG_DISTANCE =
            BETTER_FOG_LOADED || Platform.isModLoaded("simplefog");

    private FogModCompat() {
    }

    public static boolean controlsFogDistance() {
        return EXTERNAL_FOG_DISTANCE;
    }

    public static boolean controlsFogColor() {
        return BETTER_FOG_LOADED;
    }
}
