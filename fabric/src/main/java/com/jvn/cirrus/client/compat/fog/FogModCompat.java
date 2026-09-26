package com.jvn.cirrus.client.compat.fog;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientLevel;

public final class FogModCompat {
    private static final boolean BETTER_FOG_LOADED = FabricLoader.getInstance().isModLoaded("betterfog");
    private static final boolean IMB11_FOG_LOADED = FabricLoader.getInstance().isModLoaded("fog");
    private static final boolean EXTERNAL_FOG_DISTANCE =
            BETTER_FOG_LOADED || IMB11_FOG_LOADED || FabricLoader.getInstance().isModLoaded("simplefog");

    private FogModCompat() {
    }

    public static boolean controlsFogDistance() {
        return EXTERNAL_FOG_DISTANCE;
    }

    public static boolean controlsFogColor() {
        return BETTER_FOG_LOADED || IMB11_FOG_LOADED;
    }

    public static float cloudWhiteningBrightness(ClientLevel level) {
        return IMB11_FOG_LOADED ? Imb11FogCompat.cloudWhiteningBrightness(level) : Float.NaN;
    }
}
