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
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudMixin {
    @Shadow private ClientLevel level;
    @Shadow private int ticks;
    @Unique private CloudStatus cirrus$lastCloudMode;
    @Unique private final Consumer<float[]> cirrus$renderCloudsIntoDistantHorizons =
            this::cirrus$renderCloudsIntoDistantHorizons;
    @Unique private final Matrix4f cirrus$dhProjectionMatrix = new Matrix4f();
    @Unique private boolean cirrus$cloudsRenderedIntoDistantHorizons;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void cirrus$beginFrame(
            GraphicsResourceAllocator allocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            Matrix4f cullingProjectionMatrix,
            GpuBufferSlice shaderFog,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci
    ) {
        CirrusShaders.preloadSamplerTextures(Minecraft.getInstance());
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        CirrusRenderContext.capture(
                level, camera, frustumMatrix, projectionMatrix, fogColor, partialTick, ticks
        );
        CirrusCloudAttachment.updateRenderTicks(ticks);
        cirrus$cloudsRenderedIntoDistantHorizons = false;
        DistantHorizonsCompat.setBeforeApplyShaderCallback(
                DistantHorizonsCompat.shouldPrioritizeCirrusClouds()
                        ? cirrus$renderCloudsIntoDistantHorizons
                        : null
        );

        CloudStatus mode = Minecraft.getInstance().options.getCloudsType();
        if (mode != cirrus$lastCloudMode) {
            if (!CirrusCloudMode.isActive(mode)) {
                CirrusRenderers.clouds().invalidate();
            }
            cirrus$lastCloudMode = mode;
        }
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void cirrus$endFrame(
            GraphicsResourceAllocator allocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            Matrix4f cullingProjectionMatrix,
            GpuBufferSlice shaderFog,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci
    ) {
        DistantHorizonsCompat.setBeforeApplyShaderCallback(null);
        CirrusRenderContext.clear();
    }

    @Redirect(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCloudsType()Lnet/minecraft/client/CloudStatus;"
            )
    )
    private CloudStatus cirrus$keepCloudPassEnabled(Options options) {
        CloudStatus status = options.getCloudsType();
        return CirrusCloudMode.isActive(status) ? CloudStatus.FAST : status;
    }

    @Unique
    private void cirrus$renderCloudsIntoDistantHorizons(float[] dhProjectionMatrix) {
        if (dhProjectionMatrix == null
                || dhProjectionMatrix.length != 16
                || cirrus$cloudsRenderedIntoDistantHorizons
                || !CirrusRenderContext.isReady()
                || !CirrusCloudMode.isActive(Minecraft.getInstance().options.getCloudsType())) {
            return;
        }

        cirrus$cloudsRenderedIntoDistantHorizons = true;
        CirrusRenderers.clouds().render(
                level,
                new com.mojang.blaze3d.vertex.PoseStack(),
                CirrusRenderContext.frustumMatrix(),
                cirrus$dhProjectionMatrix.set(dhProjectionMatrix).transpose(),
                CirrusRenderContext.partialTick(),
                ticks,
                CirrusRenderContext.camera().position().x,
                CirrusRenderContext.camera().position().y,
                CirrusRenderContext.camera().position().z
        );
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void cirrus$releaseOnWorldChange(ClientLevel newLevel, CallbackInfo ci) {
        CirrusRenderers.clouds().invalidate();
        CirrusRenderers.aurora().invalidate();
        CirrusLightningLocator.invalidate();
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void cirrus$releaseOnReload(ResourceManager resourceManager, CallbackInfo ci) {
        CirrusRenderers.clouds().invalidate();
        CirrusCloudAttachment.invalidate();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void cirrus$releaseOnShutdown(CallbackInfo ci) {
        CirrusRenderers.clouds().close();
        CirrusCloudAttachment.invalidate();
        CirrusLightningLocator.invalidate();
    }
}
