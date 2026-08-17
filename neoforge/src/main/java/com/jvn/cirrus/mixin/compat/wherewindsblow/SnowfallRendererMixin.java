package com.jvn.cirrus.mixin.compat.wherewindsblow;

import com.jvn.cirrus.client.CirrusPrecipitationCeiling;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.jvn.wherewindsblow.client.weather.SnowfallRenderer", remap = false)
public abstract class SnowfallRendererMixin {
    @Unique private static float cirrus$precipitationCeiling = Float.POSITIVE_INFINITY;

    @Inject(method = "render", at = @At("HEAD"), require = 0, remap = false)
    private static void cirrus$prepareSnowfallCeiling(
            ClientLevel level,
            LightTexture lightTexture,
            float rainLevel,
            float thunder,
            double animationTime,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo ci
    ) {
        cirrus$precipitationCeiling = CirrusPrecipitationCeiling.relative(level, cameraY);
    }

    @ModifyArg(
            method = "addFlakeQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/jvn/wherewindsblow/client/weather/SnowfallRenderer;addFlakeVertex"
                            + "(Lcom/mojang/blaze3d/vertex/BufferBuilder;DDDFFFII)V"
            ),
            index = 2,
            require = 0,
            remap = false
    )
    private static double cirrus$capSnowflakeAtClouds(double y) {
        return Math.min(y, cirrus$precipitationCeiling);
    }
}
