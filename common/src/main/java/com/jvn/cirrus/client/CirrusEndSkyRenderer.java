package com.jvn.cirrus.client;

import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.cirrus.client.util.CirrusSkyDome;
import com.jvn.cirrus.client.util.CirrusEndGlassMesh;
import com.jvn.cirrus.client.util.CirrusShaderUniforms;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CirrusEndSkyRenderer implements AutoCloseable {
    private static final ResourceLocation END_NOISE_TEXTURE =
            Cirrus.texture("environment/end_noise.png");
    private static final float DOME_RADIUS = 100.0F;
    private static final Vec3 DEFAULT_IMPACT_DIRECTION = new Vec3(0.0, 1.0, 0.0);
    private static final int AZIMUTH_SEGMENTS = 64;
    private static final int ELEVATION_SEGMENTS = 32;

    private VertexBuffer domeBuffer;
    private VertexBuffer glassBuffer;
    private VertexBuffer beamBuffer;
    private CirrusEndVeil veil;
    private Vec3 impactDirection = DEFAULT_IMPACT_DIRECTION;
    private int lastFightTick = Integer.MIN_VALUE;

    public void updateFight(ClientLevel level, boolean bossFog, int ticks) {
        if (ticks == lastFightTick) {
            return;
        }
        lastFightTick = ticks;
        if (veil == null) {
            veil = new CirrusEndVeil();
        }
        int dragonDeathTicks = 0;
        if (bossFog) {
            for (Entity entity : level.entitiesForRendering()) {
                if (entity instanceof EnderDragon dragon && dragon.dragonDeathTime > 0) {
                    dragonDeathTicks = dragon.dragonDeathTime;
                    if (veil.charge(ticks) == 0.0F) {
                        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                        Vec3 offset = dragon.position().subtract(camera);
                        impactDirection = offset.lengthSqr() > 0.001 ? offset.normalize() : DEFAULT_IMPACT_DIRECTION;
                    }
                    break;
                }
            }
        }
        veil.update(bossFog, dragonDeathTicks, ticks);
    }

    public void resetFight() {
        veil = null;
        impactDirection = DEFAULT_IMPACT_DIRECTION;
        lastFightTick = Integer.MIN_VALUE;
    }

    public void render(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            int ticks
    ) {
        prepareDome();
        ShaderInstance shader = CirrusShaders.endSky();
        int previousTexture = RenderSystem.getShaderTexture(0);
        RenderSystem.setShaderTexture(0, END_NOISE_TEXTURE);
        configureSky(shader, ticks + partialTick);

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
            renderGlass(poseStack.last().pose(), projectionMatrix, ticks + partialTick);
            float flash = veil == null ? 0.0F : veil.flash(ticks + partialTick);
            if (flash > 0.0F) {
                CirrusShaderUniforms.setUniform(shader, "CirrusEndFlash", flash);
                domeBuffer.bind();
                domeBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            }
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderTexture(0, previousTexture);
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

    private void configureSky(ShaderInstance shader, float ticks) {
        CirrusShaderUniforms.setUniform(shader, "CirrusEndFlash", 0.0F);
        CirrusShaderUniforms.setUniform(shader, "CirrusEndShatter", veil == null ? 0.0F : veil.shatter(ticks));
        CirrusShaderUniforms.setUniform(shader, "CirrusEndCharge", veil == null ? 0.0F : veil.charge(ticks));
        CirrusShaderUniforms.setUniform(
                shader, "CirrusEndImpact", (float)impactDirection.x, (float)impactDirection.y, (float)impactDirection.z
        );
        CirrusShaderUniforms.setUniform(shader, "CirrusEndTime", ticks / 20.0F);
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
    }

    private void renderGlass(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float ticks) {
        ShaderInstance shader = CirrusShaders.endGlass();
        float opacity = veil == null ? 0.0F : veil.opacity(ticks);
        CirrusShaderUniforms.setUniform(shader, "CirrusEndVeil", opacity);
        CirrusShaderUniforms.setUniform(shader, "CirrusEndShatter", veil == null ? 0.0F : veil.shatter(ticks));
        if (opacity <= 0.0F) {
            return;
        }
        if (glassBuffer == null) {
            glassBuffer = CirrusEndGlassMesh.create(DOME_RADIUS);
        }
        configureSky(shader, ticks);
        CirrusShaderUniforms.setUniform(shader, "CirrusEndBeamPass", 0.0F);
        RenderSystem.disableCull();
        try {
            glassBuffer.bind();
            glassBuffer.drawWithShader(modelViewMatrix, projectionMatrix, shader);
            if (veil.charge(ticks) > 0.0F && veil.shatter(ticks) < 0.43F) {
                if (beamBuffer == null) {
                    beamBuffer = CirrusEndGlassMesh.createBeams(DOME_RADIUS);
                }
                CirrusShaderUniforms.setUniform(shader, "CirrusEndBeamPass", 1.0F);
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                beamBuffer.bind();
                beamBuffer.drawWithShader(modelViewMatrix, projectionMatrix, shader);
            }
        } finally {
            CirrusShaderUniforms.setUniform(shader, "CirrusEndBeamPass", 0.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
        }
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
        resetFight();
        if (beamBuffer != null) {
            beamBuffer.close();
            beamBuffer = null;
        }
        if (glassBuffer != null) {
            glassBuffer.close();
            glassBuffer = null;
        }
        if (domeBuffer != null) {
            domeBuffer.close();
            domeBuffer = null;
        }
    }
}
