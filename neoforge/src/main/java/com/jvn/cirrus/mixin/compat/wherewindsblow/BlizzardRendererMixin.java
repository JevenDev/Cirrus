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
@Mixin(targets = "com.jvn.wherewindsblow.client.weather.BlizzardRenderer", remap = false)
public abstract class BlizzardRendererMixin {
    @Unique private static float cirrus$precipitationCeiling = Float.POSITIVE_INFINITY;

    @Inject(method = "addSnowSheet", at = @At("HEAD"), require = 0, remap = false)
    private static void cirrus$prepareBlowingSnowCeiling(
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
            float motionX,
            float motionZ,
            float driftX,
            float driftZ,
            float horizontalScroll,
            float offsetU,
            float offsetV,
            float alpha,
            int skyLight,
            int blockLight,
            float lateralOffset,
            CallbackInfo ci
    ) {
        cirrus$precipitationCeiling = CirrusPrecipitationCeiling.relative(
                Minecraft.getInstance().level,
                cameraY
        );
    }

    @ModifyArg(
            method = "addSnowSheet",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            index = 1,
            require = 0,
            remap = false
    )
    private static float cirrus$capBlowingSnowAtClouds(float y) {
        return Math.min(y, cirrus$precipitationCeiling);
    }
}
