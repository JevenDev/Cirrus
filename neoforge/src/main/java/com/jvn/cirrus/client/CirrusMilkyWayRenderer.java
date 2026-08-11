package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.toucanlib.client.render.ToucanSkyDome;
import com.jvn.toucanlib.neoforge.client.ToucanShaders;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
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
        boolean skyGradientsEnabled = CirrusConfig.SKY_GRADIENTS_ENABLED.get();
        if (!milkyWayEnabled && !skyGradientsEnabled) {
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
        CirrusSkyPalette.Sample skyPalette = skyGradientsEnabled
                ? CirrusSkyPalette.sample(level, partialTick)
                : null;
        float skyGradientIntensity = skyPalette != null
                ? skyPalette.strength()
                        * weatherVisibility
                        * CirrusConfig.SKY_GRADIENT_OPACITY.get().floatValue()
                : 0.0F;
        if (Math.max(milkyWayIntensity, skyGradientIntensity) < 0.002F) {
            return;
        }

        prepareDome();
        ShaderInstance shader = CirrusShaders.milkyWay();
        ToucanShaders.setUniform(shader, "CirrusMilkyWayIntensity", milkyWayIntensity);
        ToucanShaders.setUniform(
                shader, "CirrusMilkyWayPixelation", CirrusConfig.MILKY_WAY_PIXELATION_ENABLED.get()
        );
        ToucanShaders.setUniform(
                shader, "CirrusMilkyWayPixelationResolution",
                CirrusConfig.MILKY_WAY_PIXELATION_RESOLUTION.get().floatValue()
        );
        ToucanShaders.setUniform(shader, "CirrusSkyGradientIntensity", skyGradientIntensity);
        ToucanShaders.setUniform(
                shader, "CirrusSkyGradientHeight", CirrusConfig.SKY_GRADIENT_HEIGHT.get().floatValue()
        );
        if (skyPalette != null) {
            ToucanShaders.setUniform(
                    shader, "CirrusSkyHorizonColor",
                    skyPalette.horizonRed(), skyPalette.horizonGreen(), skyPalette.horizonBlue()
            );
            ToucanShaders.setUniform(
                    shader, "CirrusSkyZenithColor",
                    skyPalette.zenithRed(), skyPalette.zenithGreen(), skyPalette.zenithBlue()
            );
        }
        // Use the same smoothed celestial angle as Minecraft's sun, moon,
        // and stars so the galactic band remains attached to the sky.
        ToucanShaders.setUniform(
                shader, "CirrusMilkyWayRotation", level.getTimeOfDay(partialTick) * Mth.TWO_PI
        );

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
