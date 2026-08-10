package com.jvn.cirrus.mixin.compat.wherewindsblow;

import com.jvn.cirrus.client.CirrusPrecipitationCeiling;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.jvn.wherewindsblow.client.weather.WindReactivePrecipitationRenderer", remap = false)
public abstract class WindReactivePrecipitationRendererMixin {
    @Unique private static float cirrus$precipitationCeiling = Float.POSITIVE_INFINITY;

    @Inject(method = "addRainQuad", at = @At("HEAD"), require = 0, remap = false)
    private static void cirrus$prepareRainCeiling(
            BufferBuilder buffer,
            int x,
            int z,
            int bottomY,
            int topY,
            double cameraX,
            double cameraY,
            double cameraZ,
            double widthX,
            double widthZ,
            float driftX,
            float driftZ,
            float scroll,
            float alpha,
            int light,
            float offset,
            CallbackInfo ci
    ) {
        cirrus$precipitationCeiling = CirrusPrecipitationCeiling.relative(
                Minecraft.getInstance().level,
                cameraY
        );
    }

    @ModifyArg(
            method = "addRainQuad",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            index = 1,
            require = 0,
            remap = false
    )
    private static float cirrus$capRainAtClouds(float y) {
        return Math.min(y, cirrus$precipitationCeiling);
    }
}
