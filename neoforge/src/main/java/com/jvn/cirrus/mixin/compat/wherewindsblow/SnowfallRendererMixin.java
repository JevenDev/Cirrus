package com.jvn.cirrus.mixin.compat.wherewindsblow;

import com.jvn.cirrus.client.CirrusPrecipitationCeiling;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

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

    @ModifyArgs(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/jvn/wherewindsblow/client/weather/SnowfallRenderer;addFlakeQuad"
                            + "(Lcom/mojang/blaze3d/vertex/BufferBuilder;Lorg/joml/Vector3f;"
                            + "Lorg/joml/Vector3f;DDDFFFII"
                            + "Lcom/jvn/wherewindsblow/client/weather/SnowfallRenderer$FlakeUv;)V"
            ),
            require = 0,
            remap = false
    )
    private static void cirrus$hideSnowflakesAboveClouds(Args args) {
        if (!Float.isFinite(cirrus$precipitationCeiling)) {
            return;
        }

        Vector3f left = args.get(1);
        Vector3f up = args.get(2);
        double centerY = args.get(4);
        float halfSize = args.get(6);
        float tilt = args.get(7);
        float cosine = (float)Math.cos(tilt);
        float sine = (float)Math.sin(tilt);
        float widthY = (left.y() * cosine + up.y() * sine) * halfSize;
        float heightScale = 1.0F + halfSize * 1.8F;
        float heightY = (up.y() * cosine - left.y() * sine) * halfSize * heightScale;
        double topY = centerY + Math.abs(widthY) + Math.abs(heightY);
        if (topY > cirrus$precipitationCeiling) {
            args.set(8, 0.0F);
        }
    }
}
