package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class CirrusEndSkyRenderer implements AutoCloseable {
    private static final float DOME_RADIUS = 100.0F;
    private static final int AZIMUTH_SEGMENTS = 64;
    private static final int ELEVATION_SEGMENTS = 32;

    private VertexBuffer domeBuffer;

    public void render(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            int ticks
    ) {
        prepareDome();
        ShaderInstance shader = CirrusShaders.endSky();
        setUniform(shader, "CirrusEndTime", (ticks + partialTick) / 20.0F);
        setUniform(shader, "CirrusEndIntensity", CirrusConfig.END_SKY_INTENSITY.get().floatValue());
        setUniform(shader, "CirrusEndAnimationSpeed", CirrusConfig.END_SKY_ANIMATION_SPEED.get().floatValue());
        setUniform(shader, "CirrusEndMorphSpeed", CirrusConfig.END_SKY_MORPH_SPEED.get().floatValue());
        setUniform(shader, "CirrusEndVoidCoverage", CirrusConfig.END_SKY_VOID_COVERAGE.get().floatValue());
        setUniform(shader, "CirrusEndVoidDarkness", CirrusConfig.END_SKY_VOID_DARKNESS.get().floatValue());
        setUniform(shader, "CirrusEndLightningFrequency", CirrusConfig.END_SKY_LIGHTNING_FREQUENCY.get().floatValue());
        setUniform(shader, "CirrusEndLightningIntensity", CirrusConfig.END_SKY_LIGHTNING_INTENSITY.get().floatValue());
        setUniform(shader, "CirrusEndSurgeFrequency", CirrusConfig.END_SKY_SURGE_FREQUENCY.get().floatValue());
        setUniform(shader, "CirrusEndSurgeStrength", CirrusConfig.END_SKY_SURGE_STRENGTH.get().floatValue());
        setUniform(shader, "CirrusEndPixelation", CirrusConfig.END_SKY_PIXELATION_ENABLED.get() ? 1.0F : 0.0F);

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        float[] previousColor = RenderSystem.getShaderColor();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            domeBuffer.bind();
            domeBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(
                    previousColor[0],
                    previousColor[1],
                    previousColor[2],
                    previousColor[3]
            );
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    private static void setUniform(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
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
                    elevationIndex / (float)ELEVATION_SEGMENTS,
                    -Mth.HALF_PI,
                    Mth.HALF_PI
            );
            float elevation1 = Mth.lerp(
                    (elevationIndex + 1) / (float)ELEVATION_SEGMENTS,
                    -Mth.HALF_PI,
                    Mth.HALF_PI
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
