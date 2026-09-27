package com.jvn.cirrus.client.compat.fog;

import dev.architectury.platform.Platform;
import net.minecraft.client.multiplayer.ClientLevel;

public final class FogModCompat {
    private static final boolean BETTER_FOG_LOADED = Platform.isModLoaded("betterfog");
    private static final boolean IMB11_FOG_LOADED = Platform.isModLoaded("fog");
    private static final boolean EXTERNAL_FOG_DISTANCE =
            BETTER_FOG_LOADED || IMB11_FOG_LOADED || Platform.isModLoaded("simplefog");
    private static final boolean POLYTONE_FOG_INTEGRATION_SUPPORTED =
            IMB11_FOG_LOADED && hasLegacyPolytoneBiomeManager();

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

    public static boolean supportsPolytoneFogIntegration() {
        return POLYTONE_FOG_INTEGRATION_SUPPORTED;
    }

    private static boolean hasLegacyPolytoneBiomeManager() {
        if (!Platform.isModLoaded("polytone")) {
            return false;
        }
        try {
            Class<?> polytone = Class.forName(
                    "net.mehvahdjukaar.polytone.Polytone", false, FogModCompat.class.getClassLoader());
            return polytone.getDeclaredField("BIOME_MODIFIERS").getType().getName()
                    .equals("net.mehvahdjukaar.polytone.biome.BiomeEffectsManager");
        } catch (ClassNotFoundException | NoSuchFieldException exception) {
            return false;
        }
    }
}
