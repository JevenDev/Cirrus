package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.cirrus.client.util.CirrusSkyDome;
import com.jvn.cirrus.client.util.CirrusShaderUniforms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.jvn.cirrus.client.render.CirrusVertexBuffer;
import com.jvn.cirrus.client.render.CirrusShader;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class CirrusEndSkyRenderer implements AutoCloseable {
    private static final float DOME_RADIUS = 100.0F;
    private static final int AZIMUTH_SEGMENTS = 64;
    private static final int ELEVATION_SEGMENTS = 32;

    private CirrusVertexBuffer domeBuffer;

    public void render(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            int ticks
    ) {
        prepareDome();
        CirrusShader shader = CirrusShaders.endSky();
        CirrusShaderUniforms.setUniform(shader, "CirrusEndTime", (ticks + partialTick) / 20.0F);
        CirrusShaderUniforms.setUniform(
                shader, "CirrusEndNoiseOctaves", (float)CirrusConfig.END_SKY_QUALITY.get().noiseOctaves()
        );
        CirrusShaderUniforms.setUniform(shader, "CirrusEndIntensity", CirrusConfig.END_SKY_INTENSITY.get().floatValue());
        CirrusShaderUniforms.setUniform(
                shader, "CirrusEndAnimationSpeed", CirrusConfig.END_SKY_ANIMATION_SPEED.get().floatValue()
        );
        CirrusShaderUniforms.setUniform(shader, "CirrusEndMorphSpeed", CirrusConfig.END_SKY_MORPH_SPEED.get().floatValue());
        CirrusShaderUniforms.setUniform(shader, "CirrusEndVoidCoverage", CirrusConfig.END_SKY_VOID_COVERAGE.get().floatValue());
        CirrusShaderUniforms.setUniform(shader, "CirrusEndVoidDarkness", CirrusConfig.END_SKY_VOID_DARKNESS.get().floatValue());
        CirrusShaderUniforms.setUniform(
                shader, "CirrusEndLightningFrequency", CirrusConfig.END_SKY_LIGHTNING_FREQUENCY.get().floatValue()
        );
        CirrusShaderUniforms.setUniform(
                shader, "CirrusEndLightningIntensity", CirrusConfig.END_SKY_LIGHTNING_INTENSITY.get().floatValue()
        );
        CirrusShaderUniforms.setUniform(
                shader, "CirrusEndSurgeFrequency", CirrusConfig.END_SKY_SURGE_FREQUENCY.get().floatValue()
        );
        CirrusShaderUniforms.setUniform(
                shader, "CirrusEndSurgeStrength", CirrusConfig.END_SKY_SURGE_STRENGTH.get().floatValue()
        );
        CirrusShaderUniforms.setUniform(shader, "CirrusEndPixelation", CirrusConfig.END_SKY_PIXELATION_ENABLED.get());
        CirrusShaderUniforms.setUniform(
                shader, "CirrusEndPixelationResolution", CirrusConfig.END_SKY_PIXELATION_RESOLUTION.get().floatValue()
        );

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        domeBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
    }

    private void prepareDome() {
        if (domeBuffer == null) {
            domeBuffer = CirrusSkyDome.create(
                    DOME_RADIUS, AZIMUTH_SEGMENTS, ELEVATION_SEGMENTS, -Mth.HALF_PI, Mth.HALF_PI
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
