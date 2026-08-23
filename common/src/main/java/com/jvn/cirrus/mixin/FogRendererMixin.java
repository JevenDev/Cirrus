package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusSkyPalette;
import com.jvn.cirrus.client.util.CirrusEasing;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    private static final float MATCHED_TERRAIN_FADE_FRACTION = 0.30F;

    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void cirrus$useHorizonFogColor(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistanceChunks,
            float darkenWorldAmount,
            CallbackInfo ci
    ) {
        if (!shouldMatchSky(camera, level)) {
            return;
        }

        CirrusSkyPalette.Sample palette = CirrusSkyPalette.sample(level, partialTick);
        float rain = Mth.clamp(level.getRainLevel(partialTick), 0.0F, 1.0F);
        float thunder = Mth.clamp(level.getThunderLevel(partialTick), 0.0F, 1.0F);
        float weatherVisibility = (1.0F - rain * 0.88F) * (1.0F - thunder * 0.12F);
        float gradientVisibility = Mth.clamp(
                palette.strength()
                        * CirrusConfig.SKY_GRADIENT_OPACITY.get().floatValue()
                        * weatherVisibility,
                0.0F,
                1.0F
        );
        if (gradientVisibility < 0.002F) {
            return;
        }

        float strength = Mth.clamp(
                CirrusConfig.FOG_HORIZON_TINT_STRENGTH.get().floatValue(),
                0.0F,
                1.0F
        );
        applyHorizonColorCorrection(
                camera,
                partialTick,
                level,
                renderDistanceChunks,
                palette,
                gradientVisibility,
                strength
        );
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void cirrus$softenTerrainFogTransition(
            Camera camera,
            FogRenderer.FogMode fogMode,
            float farPlaneDistance,
            boolean foggy,
            float partialTick,
            CallbackInfo ci
    ) {
        if (fogMode != FogRenderer.FogMode.FOG_TERRAIN
                || foggy
                || !(camera.getEntity().level() instanceof ClientLevel level)
                || !shouldMatchSky(camera, level)) {
            return;
        }

        float strength = Mth.clamp(
                CirrusConfig.FOG_HORIZON_TINT_STRENGTH.get().floatValue(),
                0.0F,
                1.0F
        );
        if (strength < 0.002F) {
            return;
        }

        float vanillaFadeDistance = Mth.clamp(
                farPlaneDistance / 10.0F,
                4.0F,
                64.0F
        );
        float matchedFadeDistance = Math.max(
                vanillaFadeDistance,
                farPlaneDistance * MATCHED_TERRAIN_FADE_FRACTION
        );
        float fadeDistance = Mth.lerp(strength, vanillaFadeDistance, matchedFadeDistance);
        RenderSystem.setShaderFogStart(Math.max(0.0F, farPlaneDistance - fadeDistance));
    }

    private static void applyHorizonColorCorrection(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistanceChunks,
            CirrusSkyPalette.Sample palette,
            float gradientVisibility,
            float strength
    ) {
        Vec3 vanillaSkyColor = level.getSkyColor(camera.getPosition(), partialTick);
        float viewElevation = camera.getLookVector().y();
        float elevation = Mth.clamp(viewElevation, 0.0F, 1.0F);
        float gradientAmount = smoothstep(
                0.015F,
                Math.max(CirrusConfig.SKY_GRADIENT_HEIGHT.get().floatValue(), 0.02F),
                elevation
        );
        float horizonColorBoost = 1.0F - smoothstep(0.02F, 0.32F, elevation);
        float skyDomeFade = smoothstep(-0.18F, 0.02F, viewElevation);
        float gradientAlpha = Mth.clamp(
                gradientVisibility
                        * Mth.lerp(horizonColorBoost, 0.22F, 0.38F)
                        * skyDomeFade,
                0.0F,
                1.0F
        );
        float gradientRed = Mth.lerp(
                gradientAmount, palette.horizonRed(), palette.zenithRed()
        );
        float gradientGreen = Mth.lerp(
                gradientAmount, palette.horizonGreen(), palette.zenithGreen()
        );
        float gradientBlue = Mth.lerp(
                gradientAmount, palette.horizonBlue(), palette.zenithBlue()
        );
        float vanillaRed = (float)vanillaSkyColor.x;
        float vanillaGreen = (float)vanillaSkyColor.y;
        float vanillaBlue = (float)vanillaSkyColor.z;
        float red = Mth.lerp(gradientAlpha, vanillaRed, gradientRed);
        float green = Mth.lerp(gradientAlpha, vanillaGreen, gradientGreen);
        float blue = Mth.lerp(gradientAlpha, vanillaBlue, gradientBlue);

        if (renderDistanceChunks >= 4) {
            float[] sunriseColor = level.effects().getSunriseColor(
                    level.getTimeOfDay(partialTick), partialTick
            );
            if (sunriseColor != null) {
                float sunDirection = Mth.sin(level.getSunAngle(partialTick)) > 0.0F
                        ? -1.0F
                        : 1.0F;
                float sunriseAlpha = Mth.clamp(
                        camera.getLookVector().x() * sunDirection,
                        0.0F,
                        1.0F
                ) * sunriseColor[3];
                red = Mth.lerp(sunriseAlpha, red, sunriseColor[0]);
                green = Mth.lerp(sunriseAlpha, green, sunriseColor[1]);
                blue = Mth.lerp(sunriseAlpha, blue, sunriseColor[2]);
            }
        }

        fogRed = Mth.clamp(Mth.lerp(strength, fogRed, red), 0.0F, 1.0F);
        fogGreen = Mth.clamp(Mth.lerp(strength, fogGreen, green), 0.0F, 1.0F);
        fogBlue = Mth.clamp(Mth.lerp(strength, fogBlue, blue), 0.0F, 1.0F);
    }

    private static float smoothstep(float lowerEdge, float upperEdge, float value) {
        float amount = Mth.clamp((value - lowerEdge) / (upperEdge - lowerEdge), 0.0F, 1.0F);
        return CirrusEasing.smoothstep(amount);
    }

    private static boolean shouldMatchSky(Camera camera, ClientLevel level) {
        return CirrusConfig.FOG_USES_HORIZON_COLOR.get()
                && CirrusConfig.SKY_GRADIENTS_ENABLED.get()
                && camera.getFluidInCamera() == FogType.NONE
                && level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL
                && !hasVisibilityEffect(camera);
    }

    private static boolean hasVisibilityEffect(Camera camera) {
        return camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS)
                        || living.hasEffect(MobEffects.DARKNESS));
    }
}
