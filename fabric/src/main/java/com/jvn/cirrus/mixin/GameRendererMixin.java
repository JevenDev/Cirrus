package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusLightningLocator;
import com.jvn.cirrus.client.CirrusRenderers;
import com.jvn.cirrus.client.CirrusTimeTransition;
import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "extract", at = @At("HEAD"))
    private void cirrus$beginTimeTransitionExtraction(
            DeltaTracker deltaTracker,
            boolean advanceGameTime,
            CallbackInfo ci
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (advanceGameTime && level != null) {
            CirrusTimeTransition.beginFrame(level);
        }
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void cirrus$endTimeTransitionExtraction(
            DeltaTracker deltaTracker,
            boolean advanceGameTime,
            CallbackInfo ci
    ) {
        CirrusTimeTransition.endFrame();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void cirrus$releaseOnWorldChange(ClientLevel newLevel, CallbackInfo ci) {
        CirrusRenderers.clouds().invalidate();
        CirrusRenderers.aurora().invalidate();
        CirrusLightningLocator.invalidate();
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"
            )
    )
    private void cirrus$captureWorldProjection(
            DeltaTracker deltaTracker,
            CallbackInfo ci,
            @Local Matrix4f projectionMatrix
    ) {
        CirrusRenderContext.captureWorldProjection(projectionMatrix);
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void cirrus$beginTimeTransitionFrame(DeltaTracker deltaTracker, CallbackInfo ci) {
        DistantHorizonsCompat.beginFrame();
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            CirrusTimeTransition.beginFrame(level);
        }
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void cirrus$endTimeTransitionFrame(DeltaTracker deltaTracker, CallbackInfo ci) {
        CirrusTimeTransition.endFrame();
    }
}
