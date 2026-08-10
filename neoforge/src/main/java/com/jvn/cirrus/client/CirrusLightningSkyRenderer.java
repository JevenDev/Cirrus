package com.jvn.cirrus.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
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
        Uniform directionUniform = shader.getUniform("CirrusLightningSkyDirection");
        if (directionUniform != null) {
            directionUniform.set((float)flashDirection.x, (float)flashDirection.y, (float)flashDirection.z);
        }
        Uniform intensityUniform = shader.getUniform("CirrusLightningSkyIntensity");
        if (intensityUniform != null) {
            intensityUniform.set(intensity);
        }

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
        if (domeBuffer != null) {
            return;
        }

        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION
        );
        for (int elevationIndex = 0; elevationIndex < ELEVATION_SEGMENTS; elevationIndex++) {
            float elevation0 = Mth.lerp(
                    elevationIndex / (float)ELEVATION_SEGMENTS, MIN_ELEVATION, MAX_ELEVATION
            );
            float elevation1 = Mth.lerp(
                    (elevationIndex + 1) / (float)ELEVATION_SEGMENTS, MIN_ELEVATION, MAX_ELEVATION
            );
            for (int azimuthIndex = 0; azimuthIndex < AZIMUTH_SEGMENTS; azimuthIndex++) {
                float azimuth0 = azimuthIndex * Mth.TWO_PI / AZIMUTH_SEGMENTS;
                float azimuth1 = (azimuthIndex + 1) * Mth.TWO_PI / AZIMUTH_SEGMENTS;
                addDomeVertex(builder, azimuth0, elevation0);
                addDomeVertex(builder, azimuth0, elevation1);
                addDomeVertex(builder, azimuth1, elevation1);
                addDomeVertex(builder, azimuth1, elevation0);
            }
        }

        MeshData mesh = builder.buildOrThrow();
        domeBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        domeBuffer.bind();
        domeBuffer.upload(mesh);
        VertexBuffer.unbind();
    }

    private static void addDomeVertex(BufferBuilder builder, float azimuth, float elevation) {
        float horizontal = Mth.cos(elevation) * DOME_RADIUS;
        builder.addVertex(
                Mth.sin(azimuth) * horizontal,
                Mth.sin(elevation) * DOME_RADIUS,
                Mth.cos(azimuth) * horizontal
        );
    }

    @Override
    public void close() {
        if (domeBuffer != null) {
            domeBuffer.close();
            domeBuffer = null;
        }
    }
}
