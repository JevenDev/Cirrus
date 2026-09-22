package com.jvn.cirrus.client.util;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CirrusCelestialRenderState {
    private static final Matrix4f INVERSE_VIEW = new Matrix4f();
    private static final Matrix4f SUN_MODEL_VIEW = new Matrix4f();
    private static final Vector3f SUN_DIRECTION = new Vector3f();
    private static final Vector3f MOON_DIRECTION = new Vector3f();
    private static boolean sunCaptured;
    private static boolean moonCaptured;

    private CirrusCelestialRenderState() {
    }

    public static void beginFrame(Matrix4f view) {
        INVERSE_VIEW.set(view).invert();
        sunCaptured = false;
        moonCaptured = false;
    }

    public static void captureSun(Matrix4f modelView) {
        SUN_MODEL_VIEW.set(modelView);
        modelView.transformDirection(SUN_DIRECTION.set(0.0F, 1.0F, 0.0F));
        INVERSE_VIEW.transformDirection(SUN_DIRECTION).normalize();
        sunCaptured = true;
    }

    public static void captureMoon(Matrix4f modelView) {
        modelView.transformDirection(MOON_DIRECTION.set(0.0F, -1.0F, 0.0F));
        INVERSE_VIEW.transformDirection(MOON_DIRECTION).normalize();
        moonCaptured = true;
    }

    public static Matrix4f sunModelView() {
        return sunCaptured ? SUN_MODEL_VIEW : null;
    }

    public static Vector3f sunDirection(Vector3f destination, float timeOfDay, boolean angledOrbit) {
        return sunCaptured ? destination.set(SUN_DIRECTION)
                : CirrusCelestialTransform.sunDirection(destination, timeOfDay, angledOrbit);
    }

    public static Vector3f moonDirection(Vector3f destination, float timeOfDay, boolean angledOrbit) {
        return moonCaptured ? destination.set(MOON_DIRECTION)
                : CirrusCelestialTransform.moonDirection(destination, timeOfDay, angledOrbit);
    }
}
