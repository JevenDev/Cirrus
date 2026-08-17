package com.jvn.cirrus.mixin.compat.wherewindsblow;

import com.jvn.cirrus.client.CirrusPrecipitationCeiling;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.jvn.wherewindsblow.client.weather.DesertStormRenderer", remap = false)
public abstract class DesertStormRendererMixin {
    @Unique private static float cirrus$relativePrecipitationCeiling = Float.POSITIVE_INFINITY;

    @Inject(method = "render", at = @At("HEAD"), require = 0, remap = false)
    private static void cirrus$prepareDustCeiling(
            ClientLevel level,
            float rainLevel,
            float thunder,
            double animationTime,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo ci
    ) {
        cirrus$relativePrecipitationCeiling =
                CirrusPrecipitationCeiling.relative(level, cameraY);
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/shaders/AbstractUniform;set(F)V",
                    ordinal = 3
            ),
            index = 0,
            require = 0,
            remap = false
    )
    private static float cirrus$hideDustAboveClouds(float opacity) {
        return cirrus$relativePrecipitationCeiling <= 0.0F ? 0.0F : opacity;
    }

    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/shaders/AbstractUniform;set(F)V",
                    ordinal = 5
            ),
            index = 0,
            require = 0,
            remap = false
    )
    private static float cirrus$capDustAtClouds(float verticalSpan) {
        if (!Float.isFinite(cirrus$relativePrecipitationCeiling)
                || cirrus$relativePrecipitationCeiling <= 0.0F) {
            return verticalSpan;
        }
        return Math.min(verticalSpan, cirrus$relativePrecipitationCeiling * 2.0F);
    }
}
