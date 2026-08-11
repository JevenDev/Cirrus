package com.jvn.cirrus.client;

import com.jvn.toucanlib.client.render.ToucanSkyDome;
import com.jvn.toucanlib.neoforge.client.ToucanShaders;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CirrusLightningSkyRenderer implements AutoCloseable {
    private static final float DOME_RADIUS = 100.0F;
    private static final int AZIMUTH_SEGMENTS = 96;
    private static final int ELEVATION_SEGMENTS = 32;
    private static final float MIN_ELEVATION = (float)Math.toRadians(-18.0);
    private static final float MAX_ELEVATION = (float)Math.toRadians(90.0);

    private VertexBuffer domeBuffer;

    public void render(
            ClientLevel level,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera
    ) {
        if (Minecraft.getInstance().options.hideLightningFlash().get()) {
            return;
        }

        float intensity = Mth.clamp(level.getSkyFlashTime() - partialTick, 0.0F, 1.0F);
        if (intensity < 0.002F) {
            return;
        }

        LightningBolt lightning = nearestLightning(level, camera.getPosition());
        if (lightning == null) {
            return;
        }

        Vec3 cameraPosition = camera.getPosition();
        Vec3 visualOrigin = CirrusCloudAttachment.findVisualOrigin(lightning, partialTick);
        Vec3 flashDirection = new Vec3(
                lightning.getX() + visualOrigin.x - cameraPosition.x,
                lightning.getY() + visualOrigin.y - cameraPosition.y,
                lightning.getZ() + visualOrigin.z - cameraPosition.z
        ).normalize();

        prepareDome();
        ShaderInstance shader = CirrusShaders.lightningSky();
        ToucanShaders.setUniform(
                shader, "CirrusLightningSkyDirection",
                (float)flashDirection.x, (float)flashDirection.y, (float)flashDirection.z
        );
        ToucanShaders.setUniform(shader, "CirrusLightningSkyIntensity", intensity);

        float[] previousColor = RenderSystem.getShaderColor();
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            domeBuffer.bind();
            domeBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(
                    previousColor[0], previousColor[1], previousColor[2], previousColor[3]
            );
            RenderSystem.defaultBlendFunc();
        }
    }

    private static LightningBolt nearestLightning(ClientLevel level, Vec3 cameraPosition) {
        LightningBolt nearest = null;
        double nearestDistanceSquared = Double.POSITIVE_INFINITY;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LightningBolt lightning)) {
                continue;
            }
            double distanceSquared = lightning.distanceToSqr(cameraPosition);
            if (distanceSquared < nearestDistanceSquared) {
                nearest = lightning;
                nearestDistanceSquared = distanceSquared;
            }
        }
        return nearest;
    }

    private void prepareDome() {
        if (domeBuffer == null) {
            domeBuffer = ToucanSkyDome.create(
                    DOME_RADIUS, AZIMUTH_SEGMENTS, ELEVATION_SEGMENTS, MIN_ELEVATION, MAX_ELEVATION
            );
        }
    }

    @Override
    public void close() {
        if (domeBuffer != null) {
            domeBuffer.close();
            domeBuffer = null;
        }
    }
}
