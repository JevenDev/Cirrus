package com.jvn.cirrus.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class CirrusRenderContext {
    private static ClientLevel level;
    private static Camera camera;
    private static Matrix4f frustumMatrix;
    private static Matrix4f projectionMatrix;
    private static Matrix4f pendingWorldProjectionMatrix;
    private static Vector4f fogColor;
    private static float partialTick;
    private static int ticks;
    private static float cloudHeight = Float.NaN;
    private static boolean cloudsRenderedIntoDistantHorizons;
    private static boolean renderingCloudsForDistantHorizons;

    private CirrusRenderContext() {
    }

    public static void captureWorldProjection(Matrix4f capturedProjection) {
        pendingWorldProjectionMatrix = new Matrix4f(capturedProjection);
    }

    public static void capture(
            ClientLevel capturedLevel,
            Camera capturedCamera,
            Matrix4f capturedFrustum,
            Matrix4f capturedProjection,
            Vector4f capturedFogColor,
            float capturedPartialTick,
            int capturedTicks
    ) {
        level = capturedLevel;
        camera = capturedCamera;
        frustumMatrix = new Matrix4f(capturedFrustum);
        projectionMatrix = pendingWorldProjectionMatrix != null
                ? pendingWorldProjectionMatrix
                : new Matrix4f(capturedProjection);
        pendingWorldProjectionMatrix = null;
        fogColor = new Vector4f(capturedFogColor);
        partialTick = capturedPartialTick;
        ticks = capturedTicks;
        cloudsRenderedIntoDistantHorizons = false;
        renderingCloudsForDistantHorizons = false;
        cloudHeight = capturedCamera.attributeProbe()
                .getValue(EnvironmentAttributes.CLOUD_HEIGHT, capturedPartialTick);
    }

    public static boolean isReady() {
        return level != null && camera != null && frustumMatrix != null && projectionMatrix != null;
    }

    public static ClientLevel level() {
        return level;
    }

    public static Camera camera() {
        return camera;
    }

    public static Matrix4f frustumMatrix() {
        return frustumMatrix;
    }

    public static Matrix4f projectionMatrix() {
        return projectionMatrix;
    }

    public static Vector4f fogColor() {
        return fogColor;
    }

    public static float partialTick() {
        return partialTick;
    }

    public static int ticks() {
        return ticks;
    }

    public static boolean hasVisibleClouds() {
        if (camera == null) {
            return false;
        }
        int color = camera.attributeProbe().getValue(EnvironmentAttributes.CLOUD_COLOR, partialTick);
        return ARGB.alpha(color) > 0;
    }

    public static boolean cloudsRenderedIntoDistantHorizons() {
        return cloudsRenderedIntoDistantHorizons;
    }

    public static void markCloudsRenderedIntoDistantHorizons() {
        cloudsRenderedIntoDistantHorizons = true;
    }

    public static boolean renderingCloudsForDistantHorizons() {
        return renderingCloudsForDistantHorizons;
    }

    public static void beginRenderingCloudsForDistantHorizons() {
        renderingCloudsForDistantHorizons = true;
    }

    public static void endRenderingCloudsForDistantHorizons() {
        renderingCloudsForDistantHorizons = false;
    }

    public static float sunAngle(float requestedPartialTick) {
        Camera activeCamera = camera != null ? camera : Minecraft.getInstance().gameRenderer.getMainCamera();
        return (float)Math.toRadians(
                activeCamera.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, requestedPartialTick)
        );
    }

    public static Vec3 cloudColor(float requestedPartialTick) {
        Camera activeCamera = camera != null ? camera : Minecraft.getInstance().gameRenderer.getMainCamera();
        int color = activeCamera.attributeProbe()
                .getValue(EnvironmentAttributes.CLOUD_COLOR, requestedPartialTick);
        return new Vec3(ARGB.red(color) / 255.0, ARGB.green(color) / 255.0, ARGB.blue(color) / 255.0);
    }

    public static float cloudHeight(ClientLevel requestedLevel) {
        if (requestedLevel == level && Float.isFinite(cloudHeight)) {
            return cloudHeight;
        }
        Camera mainCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 position = mainCamera.position();
        return requestedLevel.environmentAttributes().getValue(EnvironmentAttributes.CLOUD_HEIGHT, position);
    }

    public static float starBrightness(ClientLevel requestedLevel, float requestedPartialTick) {
        if (requestedLevel == level && camera != null) {
            return camera.attributeProbe().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, requestedPartialTick);
        }
        return Minecraft.getInstance().gameRenderer.getMainCamera().attributeProbe()
                .getValue(EnvironmentAttributes.STAR_BRIGHTNESS, requestedPartialTick);
    }

    public static void clear() {
        level = null;
        camera = null;
        frustumMatrix = null;
        projectionMatrix = null;
        pendingWorldProjectionMatrix = null;
        fogColor = null;
        cloudHeight = Float.NaN;
        renderingCloudsForDistantHorizons = false;
        cloudsRenderedIntoDistantHorizons = false;
    }
}
