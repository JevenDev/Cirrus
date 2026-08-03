package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class CirrusMilkyWayRenderer implements AutoCloseable {
    private static final float DOME_RADIUS = 100.0F;
    private static final int AZIMUTH_SEGMENTS = 64;
    private static final int ELEVATION_SEGMENTS = 24;
    private static final float MIN_ELEVATION = (float)Math.toRadians(-30.0);
    private static final float MAX_ELEVATION = (float)Math.toRadians(90.0);

    private VertexBuffer domeBuffer;

    public void render(
            ClientLevel level,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick
    ) {
        boolean milkyWayEnabled = CirrusConfig.MILKY_WAY_ENABLED.get();
        boolean nightSkyColorsEnabled = CirrusConfig.NIGHT_SKY_COLORS_ENABLED.get();
        if (!milkyWayEnabled && !nightSkyColorsEnabled) {
            return;
        }

        float nightStrength = Mth.clamp(level.getStarBrightness(partialTick) * 1.6F, 0.0F, 1.0F);
        float rain = Mth.clamp(level.getRainLevel(partialTick), 0.0F, 1.0F);
        float thunder = Mth.clamp(level.getThunderLevel(partialTick), 0.0F, 1.0F);
        float weatherVisibility = (1.0F - rain * 0.88F) * (1.0F - thunder * 0.12F);
        float milkyWayIntensity = milkyWayEnabled
                ? nightStrength
                        * weatherVisibility
                        * CirrusConfig.MILKY_WAY_OPACITY.get().floatValue()
                : 0.0F;
        float nightSkyIntensity = nightSkyColorsEnabled
                ? nightStrength
                        * weatherVisibility
                        * CirrusConfig.NIGHT_SKY_COLOR_OPACITY.get().floatValue()
                : 0.0F;
        if (Math.max(milkyWayIntensity, nightSkyIntensity) < 0.002F) {
            return;
        }

        prepareDome();
        ShaderInstance shader = CirrusShaders.milkyWay();
        Uniform intensityUniform = shader.getUniform("CirrusMilkyWayIntensity");
        if (intensityUniform != null) {
            intensityUniform.set(milkyWayIntensity);
        }
        Uniform pixelationUniform = shader.getUniform("CirrusMilkyWayPixelation");
        if (pixelationUniform != null) {
            pixelationUniform.set(CirrusConfig.MILKY_WAY_PIXELATION_ENABLED.get() ? 1.0F : 0.0F);
        }
        Uniform nightSkyIntensityUniform = shader.getUniform("CirrusNightSkyIntensity");
        if (nightSkyIntensityUniform != null) {
            nightSkyIntensityUniform.set(nightSkyIntensity);
        }
        Uniform rotationUniform = shader.getUniform("CirrusMilkyWayRotation");
        if (rotationUniform != null) {
            // Use the same smoothed celestial angle as Minecraft's sun, moon,
            // and stars so the galactic band remains attached to the sky.
            rotationUniform.set(level.getTimeOfDay(partialTick) * Mth.TWO_PI);
        }

        float[] previousColor = RenderSystem.getShaderColor();
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
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
                    previousColor[0],
                    previousColor[1],
                    previousColor[2],
                    previousColor[3]
            );
            RenderSystem.defaultBlendFunc();
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
                    MIN_ELEVATION,
                    MAX_ELEVATION
            );
            float elevation1 = Mth.lerp(
                    (elevationIndex + 1) / (float)ELEVATION_SEGMENTS,
                    MIN_ELEVATION,
                    MAX_ELEVATION
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
