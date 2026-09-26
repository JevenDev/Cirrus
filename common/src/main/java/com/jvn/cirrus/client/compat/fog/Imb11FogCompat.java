package com.jvn.cirrus.client.compat.fog;

import com.jvn.cirrus.Cirrus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

final class Imb11FogCompat {
    private static final CloudAccess ACCESS = findAccess();
    private static final AtomicBoolean INVOCATION_WARNING_LOGGED = new AtomicBoolean();

    private Imb11FogCompat() {
    }

    static float cloudWhiteningBrightness(ClientLevel level) {
        if (ACCESS == null
                || !(level.effects() instanceof DimensionSpecialEffects.OverworldEffects)
                || level.dimensionType().hasFixedTime()) {
            return Float.NaN;
        }
        try {
            Object config = ACCESS.getConfig().invoke(null);
            if (!ACCESS.enabled().getBoolean(config)
                    || !ACCESS.cloudWhitening().getBoolean(config)
                    || (boolean)ACCESS.disabledBiome().invoke(null)
                    || (boolean)ACCESS.disabledByShaders().invoke(null)) {
                return Float.NaN;
            }
            return (float)ACCESS.cloudColor().invoke(null, level.getDayTime() % 24000L);
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (INVOCATION_WARNING_LOGGED.compareAndSet(false, true)) {
                Cirrus.LOGGER.warn("Could not read Fog's cloud whitening settings", exception);
            }
            return Float.NaN;
        }
    }

    private static CloudAccess findAccess() {
        try {
            Class<?> config = Class.forName("dev.imb11.fog.config.FogConfig");
            Class<?> manager = Class.forName("dev.imb11.fog.client.FogManager");
            Class<?> iris = Class.forName("dev.imb11.fog.client.compat.polytone.IrisCompat");
            Class<?> clouds = Class.forName("dev.imb11.fog.client.util.math.CloudCalculator");
            return new CloudAccess(
                    config.getMethod("getInstance"),
                    config.getField("enableMod"),
                    config.getField("enableCloudWhitening"),
                    manager.getMethod("isInDisabledBiome"),
                    iris.getMethod("shouldDisableMod"),
                    clouds.getMethod("getCloudColor", long.class)
            );
        } catch (ReflectiveOperationException | LinkageError exception) {
            Cirrus.LOGGER.warn("Fog's cloud whitening integration is unavailable", exception);
            return null;
        }
    }

    private record CloudAccess(
            Method getConfig,
            Field enabled,
            Field cloudWhitening,
            Method disabledBiome,
            Method disabledByShaders,
            Method cloudColor
    ) {
    }
}
