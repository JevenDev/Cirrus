package com.jvn.cirrus.client.util;

import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CirrusCelestialTransform {
    private static final float POLAR_ELEVATION = Mth.PI * 0.25F;
    private static final float POLAR_AXIS_Y = Mth.sin(POLAR_ELEVATION);
    private static final float POLAR_AXIS_Z = -Mth.cos(POLAR_ELEVATION);
    private static final float HALF_PI = Mth.PI * 0.5F;

    private CirrusCelestialTransform() {
    }

    public static Matrix4f skyModelView(
            Matrix4f destination,
            Matrix4f fixedSkyModelView,
            float timeOfDay,
            boolean angledOrbit
    ) {
        if (!angledOrbit) {
            return vanillaModelView(destination, fixedSkyModelView, timeOfDay);
        }
        return destination.set(fixedSkyModelView).rotate(
                timeOfDay * Mth.TWO_PI,
                0.0F,
                POLAR_AXIS_Y,
                POLAR_AXIS_Z
        );
    }

    public static Matrix4f bodyModelView(
            Matrix4f destination,
            Matrix4f fixedSkyModelView,
            float timeOfDay,
            boolean angledOrbit
    ) {
        if (!angledOrbit) {
            return vanillaModelView(destination, fixedSkyModelView, timeOfDay);
        }
        return destination.set(fixedSkyModelView)
                .rotate(timeOfDay * Mth.TWO_PI, 0.0F, POLAR_AXIS_Y, POLAR_AXIS_Z)
                .rotateX(POLAR_ELEVATION);
    }

    public static Matrix4f worldToSkyTexture(
            Matrix4f destination,
            float timeOfDay,
            boolean angledOrbit
    ) {
        float inverseAngle = -timeOfDay * Mth.TWO_PI;
        if (angledOrbit) {
            return destination.rotationY(Mth.PI).rotate(
                    inverseAngle,
                    0.0F,
                    POLAR_AXIS_Y,
                    POLAR_AXIS_Z
            );
        }
        return destination.rotationY(HALF_PI)
                .rotateX(inverseAngle)
                .rotateY(HALF_PI);
    }

    public static Matrix4f sunriseModelView(
            Matrix4f destination,
            Matrix4f fixedSkyModelView,
            float timeOfDay
    ) {
        float angle = timeOfDay * Mth.TWO_PI;
        float directionX = Mth.sin(angle);
        float directionZ = POLAR_AXIS_Y * Mth.cos(angle);
        float yaw = (float)Mth.atan2(directionZ, -directionX);
        return destination.set(fixedSkyModelView)
                .rotateY(yaw)
                .rotateX(HALF_PI)
                .rotateZ(HALF_PI);
    }

    public static Vector3f sunDirection(
            Vector3f destination,
            float timeOfDay,
            boolean angledOrbit
    ) {
        float angle = timeOfDay * Mth.TWO_PI;
        float sine = Mth.sin(angle);
        float cosine = Mth.cos(angle);
        if (!angledOrbit) {
            return destination.set(-sine, cosine, 0.0F);
        }
        return destination.set(
                sine,
                POLAR_AXIS_Y * cosine,
                -POLAR_AXIS_Z * cosine
        );
    }

    public static Vector3f moonDirection(
            Vector3f destination,
            float timeOfDay,
            boolean angledOrbit
    ) {
        return sunDirection(destination, timeOfDay, angledOrbit).negate();
    }

    public static float sunViewAlignment(
            float timeOfDay,
            boolean angledOrbit,
            float viewX,
            float viewZ
    ) {
        float angle = timeOfDay * Mth.TWO_PI;
        float directionX = angledOrbit ? Mth.sin(angle) : -Mth.sin(angle);
        float directionZ = angledOrbit ? POLAR_AXIS_Y * Mth.cos(angle) : 0.0F;
        float horizontalLength = Mth.sqrt(directionX * directionX + directionZ * directionZ);
        if (horizontalLength < 1.0E-4F) {
            return 0.0F;
        }
        return (viewX * directionX + viewZ * directionZ) / horizontalLength;
    }

    public static float polarAxisY() {
        return POLAR_AXIS_Y;
    }

    public static float polarAxisZ() {
        return POLAR_AXIS_Z;
    }

    private static Matrix4f vanillaModelView(
            Matrix4f destination,
            Matrix4f fixedSkyModelView,
            float timeOfDay
    ) {
        return destination.set(fixedSkyModelView)
                .rotateY(-HALF_PI)
                .rotateX(timeOfDay * Mth.TWO_PI);
    }
}
