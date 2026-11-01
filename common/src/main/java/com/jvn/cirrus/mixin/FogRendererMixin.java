package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusSky;
import com.jvn.cirrus.client.CirrusSkyPalette;
import com.jvn.cirrus.client.compat.fog.FogModCompat;
import com.jvn.cirrus.client.util.CirrusCelestialTransform;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
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

    @Inject(method = "setupColor", at = @At("TAIL"), order = 2000)
    private static void cirrus$useHorizonFogColor(
            Camera camera,
            float partialTick,
            ClientLevel level,
            int renderDistanceChunks,
            float darkenWorldAmount,
            CallbackInfo ci
    ) {
        if (!CirrusSky.matchesFog(camera, level)) {
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
        if (FogModCompat.controlsFogDistance()
                || fogMode != FogRenderer.FogMode.FOG_TERRAIN
                || foggy
                || !(camera.getEntity().level() instanceof ClientLevel level)
                || !CirrusSky.matchesFog(camera, level)) {
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

    @WrapOperation(method = "setupColor", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getLookVector()Lorg/joml/Vector3f;"
    ))
    private static Vector3f cirrus$useHorizonViewDirection(Camera camera, Operation<Vector3f> original) {
        return CirrusSky.matchesFogColor(camera, Minecraft.getInstance().level)
                ? CirrusSky.horizonDirection(camera) : original.call(camera);
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
        float gradientAlpha = Mth.clamp(gradientVisibility * 0.38F, 0.0F, 1.0F);
        float vanillaRed = (float)vanillaSkyColor.x;
        float vanillaGreen = (float)vanillaSkyColor.y;
        float vanillaBlue = (float)vanillaSkyColor.z;
        float red = Mth.lerp(gradientAlpha, vanillaRed, palette.horizonRed());
        float green = Mth.lerp(gradientAlpha, vanillaGreen, palette.horizonGreen());
        float blue = Mth.lerp(gradientAlpha, vanillaBlue, palette.horizonBlue());

        if (renderDistanceChunks >= 4) {
            float[] sunriseColor = level.effects().getSunriseColor(
                    level.getTimeOfDay(partialTick), partialTick
            );
            if (sunriseColor != null) {
                float viewYaw = camera.getYRot() * Mth.DEG_TO_RAD;
                float sunriseAlpha = Mth.clamp(
                        CirrusCelestialTransform.sunViewAlignment(
                                level.getTimeOfDay(partialTick),
                                CirrusConfig.SUN_ANGLED_ORBIT.get(),
                                -Mth.sin(viewYaw),
                                Mth.cos(viewYaw)
                        ),
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
}
