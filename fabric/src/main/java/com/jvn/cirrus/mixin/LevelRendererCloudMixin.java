package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudAttachment;
import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.client.CirrusLightningLocator;
import com.jvn.cirrus.client.CirrusRenderers;
import com.jvn.cirrus.client.CirrusShaders;
import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudMixin {
    @Unique private CloudStatus cirrus$lastCloudMode;
    @Unique private final BiConsumer<float[], float[]> cirrus$renderCloudsIntoDistantHorizons =
            this::cirrus$renderCloudsIntoDistantHorizons;
    @Unique private final Matrix4f cirrus$dhProjectionMatrix = new Matrix4f();
    @Unique private final Matrix4f cirrus$dhModelViewMatrix = new Matrix4f();

    @Inject(method = "render", at = @At("HEAD"))
    private void cirrus$beginFrame(
            GraphicsResourceAllocator allocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            CameraRenderState cameraState,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice shaderFog,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci
    ) {
        CirrusShaders.preloadSamplerTextures(Minecraft.getInstance());
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int ticks = (int)level.getGameTime();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        CirrusRenderContext.capture(
                level, camera, new Matrix4f(modelViewMatrix),
                cameraState.projectionMatrix, fogColor, partialTick, ticks
        );
        CirrusCloudAttachment.updateRenderTicks(ticks);
        DistantHorizonsCompat.setBeforeApplyShaderCallback(
                DistantHorizonsCompat.shouldPrioritizeCirrusClouds()
                        && CirrusRenderContext.hasVisibleClouds()
                                ? cirrus$renderCloudsIntoDistantHorizons
                        : null
        );

        CloudStatus mode = Minecraft.getInstance().options.getCloudStatus();
        if (mode != cirrus$lastCloudMode) {
            if (!CirrusCloudMode.isActive(mode)) {
                CirrusRenderers.clouds().invalidate();
            }
            cirrus$lastCloudMode = mode;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void cirrus$endFrame(
            GraphicsResourceAllocator allocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            CameraRenderState cameraState,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice shaderFog,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci
    ) {
        DistantHorizonsCompat.setBeforeApplyShaderCallback(null);
        CirrusRenderContext.clear();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/state/OptionsRenderState;cloudStatus:Lnet/minecraft/client/CloudStatus;"
            )
    )
    private CloudStatus cirrus$keepCloudPassEnabled(OptionsRenderState options) {
        CloudStatus status = options.cloudStatus;
        return CirrusCloudMode.isActive(status) ? CloudStatus.FAST : status;
    }

    @Unique
    private void cirrus$renderCloudsIntoDistantHorizons(
            float[] dhProjectionMatrix,
            float[] dhModelViewMatrix
    ) {
        if (dhProjectionMatrix == null
                || dhProjectionMatrix.length != 16
                || CirrusRenderContext.cloudsRenderedIntoDistantHorizons()
                || dhModelViewMatrix == null
                || dhModelViewMatrix.length != 16
                || !CirrusRenderContext.isReady()
                || !CirrusRenderContext.hasVisibleClouds()
                || !CirrusCloudMode.isActive(Minecraft.getInstance().options.getCloudStatus())) {
            return;
        }

        CirrusRenderContext.markCloudsRenderedIntoDistantHorizons();
        CirrusRenderContext.beginRenderingCloudsForDistantHorizons();
        try {
            CirrusRenderers.clouds().render(
                    CirrusRenderContext.level(),
                    new com.mojang.blaze3d.vertex.PoseStack(),
                    cirrus$dhModelViewMatrix.set(dhModelViewMatrix).transpose(),
                    cirrus$dhProjectionMatrix.set(dhProjectionMatrix).transpose(),
                    CirrusRenderContext.partialTick(),
                    CirrusRenderContext.ticks(),
                    CirrusRenderContext.camera().position().x,
                    CirrusRenderContext.camera().position().y,
                    CirrusRenderContext.camera().position().z
            );
        } finally {
            CirrusRenderContext.endRenderingCloudsForDistantHorizons();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void cirrus$releaseOnShutdown(CallbackInfo ci) {
        CirrusRenderers.clouds().close();
        CirrusCloudAttachment.invalidate();
        CirrusLightningLocator.invalidate();
    }
}
