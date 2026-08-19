package com.jvn.cirrus.mixin.compat.wherewindsblow;

import com.jvn.cirrus.client.CirrusPrecipitationCeiling;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.jvn.wherewindsblow.client.weather.GpuSnowfallRenderer", remap = false)
public abstract class GpuSnowfallRendererMixin {
    @Unique private static final float cirrus$maxSnowflakeVerticalExtent = 1.65F;
    @Unique private static float cirrus$relativePrecipitationCeiling = Float.POSITIVE_INFINITY;

    @Inject(method = "render", at = @At("HEAD"), require = 0, remap = false)
    private static void cirrus$prepareGpuSnowfallCeiling(
            ClientLevel level,
            float rainLevel,
            float thunder,
            double animationTime,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cirrus$relativePrecipitationCeiling =
                CirrusPrecipitationCeiling.relative(level, cameraY);
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/shaders/AbstractUniform;set(FFFF)V",
                    ordinal = 1
            ),
            index = 3,
            require = 0,
            remap = false
    )
    private static float cirrus$hideGpuSnowfallAboveClouds(float opacity) {
        return cirrus$relativePrecipitationCeiling <= cirrus$maxSnowflakeVerticalExtent
                ? 0.0F
                : opacity;
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/shaders/AbstractUniform;set(F)V",
                    ordinal = 4
            ),
            index = 0,
            require = 0,
            remap = false
    )
    private static float cirrus$capGpuSnowfallAtClouds(float verticalSpan) {
        if (!Float.isFinite(cirrus$relativePrecipitationCeiling)
                || cirrus$relativePrecipitationCeiling <= cirrus$maxSnowflakeVerticalExtent) {
            return verticalSpan;
        }

        float availableSpan =
                (cirrus$relativePrecipitationCeiling - cirrus$maxSnowflakeVerticalExtent) * 2.0F;
        return Math.min(verticalSpan, availableSpan);
    }
}
