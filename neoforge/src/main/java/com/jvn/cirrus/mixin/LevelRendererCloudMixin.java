package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusAuroraRenderer;
import com.jvn.cirrus.client.CirrusCloudRenderer;
import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.client.CirrusMilkyWayRenderer;
import com.jvn.cirrus.client.CirrusShaders;
import com.jvn.cirrus.client.CirrusStarRenderer;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudMixin {
    @Shadow private ClientLevel level;
    @Shadow private int ticks;
    @Unique private final CirrusCloudRenderer cirrus$cloudRenderer = new CirrusCloudRenderer();
    @Unique private final CirrusAuroraRenderer cirrus$auroraRenderer = new CirrusAuroraRenderer();
    @Unique private final CirrusMilkyWayRenderer cirrus$milkyWayRenderer = new CirrusMilkyWayRenderer();
    @Unique private final CirrusStarRenderer cirrus$starRenderer = new CirrusStarRenderer();
    @Unique private CloudStatus cirrus$lastCloudMode;
    @Unique private boolean cirrus$celestialMaskActive;

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
            if (!CirrusCloudMode.isActive(mode)) {
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
        if (!CirrusCloudMode.isActive(Minecraft.getInstance().options.getCloudsType())) {
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

    @Inject(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(" +
                            "Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;" +
                            "Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;" +
                            "Lcom/mojang/blaze3d/platform/GlStateManager$SourceFactor;" +
                            "Lcom/mojang/blaze3d/platform/GlStateManager$DestFactor;)V"
            )
    )
    private void cirrus$renderAurora(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable skyFogSetup,
            CallbackInfo ci
    ) {
        if (level != null) {
            cirrus$milkyWayRenderer.render(level, frustumMatrix, projectionMatrix, partialTick);
            cirrus$auroraRenderer.render(level, frustumMatrix, projectionMatrix, partialTick, ticks, camera);
        }
    }

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(" +
                            "Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;" +
                            "Lnet/minecraft/client/renderer/ShaderInstance;)V",
                    ordinal = 1
            )
    )
    private void cirrus$renderShaderStars(
            VertexBuffer vanillaStars,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            net.minecraft.client.renderer.ShaderInstance vanillaShader,
            Matrix4f frustumMatrix,
            Matrix4f renderSkyProjectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable skyFogSetup
    ) {
        if (level != null && CirrusConfig.CUSTOM_STARS_ENABLED.get()) {
            cirrus$starRenderer.render(
                    level,
                    modelViewMatrix,
                    projectionMatrix,
                    frustumMatrix,
                    partialTick,
                    ticks
            );
        } else {
            vanillaStars.drawWithShader(modelViewMatrix, projectionMatrix, vanillaShader);
        }
    }

    @Inject(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void cirrus$maskMoonHaloBehindClouds(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable skyFogSetup,
            CallbackInfo ci
    ) {
        if (level == null || !CirrusCloudMode.isActive(Minecraft.getInstance().options.getCloudsType())) {
            return;
        }
        cirrus$cloudRenderer.renderCelestialMask(
                level,
                frustumMatrix,
                projectionMatrix,
                partialTick,
                ticks,
                camera.getPosition().x,
                camera.getPosition().y,
                camera.getPosition().z
        );
        RenderSystem.setShader(CirrusShaders::moonOcclusion);
        cirrus$celestialMaskActive = true;
    }

    @Inject(method = "renderSky", at = @At("TAIL"))
    private void cirrus$clearCelestialCloudMask(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable skyFogSetup,
            CallbackInfo ci
    ) {
        if (cirrus$celestialMaskActive) {
            RenderSystem.depthMask(true);
            RenderSystem.clear(256, Minecraft.ON_OSX);
            cirrus$celestialMaskActive = false;
        }
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void cirrus$releaseCloudsOnWorldChange(ClientLevel newLevel, CallbackInfo ci) {
        cirrus$cloudRenderer.invalidate();
        cirrus$auroraRenderer.invalidate();
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void cirrus$releaseCloudsOnReload(ResourceManager resourceManager, CallbackInfo ci) {
        cirrus$cloudRenderer.invalidate();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void cirrus$releaseCloudsOnShutdown(CallbackInfo ci) {
        cirrus$cloudRenderer.close();
        cirrus$auroraRenderer.close();
        cirrus$milkyWayRenderer.close();
        cirrus$starRenderer.close();
    }
}
