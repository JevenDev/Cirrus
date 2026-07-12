package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudRenderer;
import com.jvn.cirrus.client.CirrusCloudMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudMixin {
    @Shadow private ClientLevel level;
    @Shadow private int ticks;
    @Unique private final CirrusCloudRenderer cirrus$cloudRenderer = new CirrusCloudRenderer();
    @Unique private CloudStatus cirrus$lastCloudMode;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void cirrus$noticeCloudModeChange(
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightTexture lightTexture,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci
    ) {
        CloudStatus mode = Minecraft.getInstance().options.getCloudsType();
        if (mode != cirrus$lastCloudMode) {
            if (mode != CirrusCloudMode.CIRRUS) {
                cirrus$cloudRenderer.invalidate();
            }
            cirrus$lastCloudMode = mode;
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void cirrus$renderExtendedClouds(
            PoseStack poseStack,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo ci
    ) {
        if (Minecraft.getInstance().options.getCloudsType() != CirrusCloudMode.CIRRUS) {
            CirrusCloudRenderer.disableShaderEffects();
            return;
        }
        ci.cancel();
        if (level != null) {
            cirrus$cloudRenderer.render(
                    level,
                    poseStack,
                    frustumMatrix,
                    projectionMatrix,
                    partialTick,
                    ticks,
                    cameraX,
                    cameraY,
                    cameraZ
            );
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void cirrus$releaseCloudsOnWorldChange(ClientLevel newLevel, CallbackInfo ci) {
        cirrus$cloudRenderer.invalidate();
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void cirrus$releaseCloudsOnReload(ResourceManager resourceManager, CallbackInfo ci) {
        cirrus$cloudRenderer.invalidate();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void cirrus$releaseCloudsOnShutdown(CallbackInfo ci) {
        cirrus$cloudRenderer.close();
    }
}
