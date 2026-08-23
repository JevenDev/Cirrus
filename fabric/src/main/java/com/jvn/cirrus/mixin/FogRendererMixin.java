package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.client.CirrusSkyPalette;
import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import com.jvn.cirrus.client.util.CirrusEasing;
import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    private static final float MATCHED_TERRAIN_FADE_FRACTION = 0.30F;

    private float cirrus$terrainFadeStrength;
    private float cirrus$farPlaneDistance;

    @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private void cirrus$useHorizonFogColor(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistanceChunks,
            float darkenWorldAmount,
            CallbackInfoReturnable<Vector4f> cir
    ) {
        Vector4f fogColor = cir.getReturnValue();
        cirrus$terrainFadeStrength = 0.0F;
        cirrus$farPlaneDistance = renderDistanceChunks * 16.0F;
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
        cirrus$terrainFadeStrength = strength;
        applyHorizonColorCorrection(
                camera,
                partialTick,
                level,
                renderDistanceChunks,
                palette,
                gradientVisibility,
                strength,
                fogColor
        );
        cir.setReturnValue(fogColor);
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void cirrus$useConfiguredCloudDistance(
            Camera camera,
            int renderDistanceChunks,
            DeltaTracker deltaTracker,
            float darkenWorldAmount,
            ClientLevel level,
            CallbackInfoReturnable<FogData> cir
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!CirrusCloudMode.isActive(minecraft.options.getCloudStatus())
                || camera.getFluidInCamera() != FogType.NONE
                || hasVisibilityEffect(camera)
                || minecraft.gui.getBossOverlay().shouldCreateWorldFog()) {
            return;
        }

        cir.getReturnValue().cloudEnd =
                DistantHorizonsCompat.cloudRenderDistanceChunks() * 16.0F;
    }

    @ModifyArg(
            method = "setupFog",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
            ),
            index = 5
    )
    private float cirrus$softenTerrainFogTransition(float vanillaStart) {
        if (cirrus$terrainFadeStrength < 0.002F) {
            return vanillaStart;
        }

        float vanillaFadeDistance = cirrus$farPlaneDistance - vanillaStart;
        float matchedFadeDistance = Math.max(
                vanillaFadeDistance,
                cirrus$farPlaneDistance * MATCHED_TERRAIN_FADE_FRACTION
        );
        float fadeDistance = Mth.lerp(
                cirrus$terrainFadeStrength,
                vanillaFadeDistance,
                matchedFadeDistance
        );
        return Math.max(0.0F, cirrus$farPlaneDistance - fadeDistance);
    }

    private static void applyHorizonColorCorrection(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistanceChunks,
            CirrusSkyPalette.Sample palette,
            float gradientVisibility,
            float strength,
            Vector4f fogColor
    ) {
        int vanillaSkyColor = camera.attributeProbe()
                .getValue(EnvironmentAttributes.SKY_COLOR, partialTick);
        float viewElevation = camera.forwardVector().y();
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
        float vanillaRed = ARGB.redFloat(vanillaSkyColor);
        float vanillaGreen = ARGB.greenFloat(vanillaSkyColor);
        float vanillaBlue = ARGB.blueFloat(vanillaSkyColor);
        float red = Mth.lerp(gradientAlpha, vanillaRed, gradientRed);
        float green = Mth.lerp(gradientAlpha, vanillaGreen, gradientGreen);
        float blue = Mth.lerp(gradientAlpha, vanillaBlue, gradientBlue);

        if (renderDistanceChunks >= 4) {
            int sunriseColor = camera.attributeProbe()
                    .getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partialTick);
            float sunriseColorAlpha = ARGB.alphaFloat(sunriseColor);
            if (sunriseColorAlpha > 0.0F) {
                float sunDirection = Mth.sin(CirrusRenderContext.sunAngle(partialTick)) > 0.0F
                        ? -1.0F
                        : 1.0F;
                float sunriseAlpha = Mth.clamp(
                        camera.forwardVector().x() * sunDirection,
                        0.0F,
                        1.0F
                ) * sunriseColorAlpha;
                red = Mth.lerp(sunriseAlpha, red, ARGB.redFloat(sunriseColor));
                green = Mth.lerp(sunriseAlpha, green, ARGB.greenFloat(sunriseColor));
                blue = Mth.lerp(sunriseAlpha, blue, ARGB.blueFloat(sunriseColor));
            }
        }

        fogColor.set(
                Mth.clamp(Mth.lerp(strength, fogColor.x, red), 0.0F, 1.0F),
                Mth.clamp(Mth.lerp(strength, fogColor.y, green), 0.0F, 1.0F),
                Mth.clamp(Mth.lerp(strength, fogColor.z, blue), 0.0F, 1.0F),
                fogColor.w
        );
    }

    private static float smoothstep(float lowerEdge, float upperEdge, float value) {
        float amount = Mth.clamp((value - lowerEdge) / (upperEdge - lowerEdge), 0.0F, 1.0F);
        return CirrusEasing.smoothstep(amount);
    }

    private static boolean shouldMatchSky(Camera camera, ClientLevel level) {
        return CirrusConfig.FOG_USES_HORIZON_COLOR.get()
                && CirrusConfig.SKY_GRADIENTS_ENABLED.get()
                && camera.getFluidInCamera() == FogType.NONE
                && Level.OVERWORLD.equals(level.dimension())
                && !hasVisibilityEffect(camera);
    }

    private static boolean hasVisibilityEffect(Camera camera) {
        return camera.entity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS)
                        || living.hasEffect(MobEffects.DARKNESS));
    }
}
