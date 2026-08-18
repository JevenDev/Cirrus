package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusAuroraRenderer;
import com.jvn.cirrus.client.CirrusCloudRenderer;
import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.client.CirrusCloudAttachment;
import com.jvn.cirrus.client.CirrusEndSkyRenderer;
import com.jvn.cirrus.client.CirrusMilkyWayRenderer;
import com.jvn.cirrus.client.CirrusLightningLocator;
import com.jvn.cirrus.client.CirrusLightningSkyRenderer;
import com.jvn.cirrus.client.CirrusPrecipitationCeiling;
import com.jvn.cirrus.client.CirrusShaders;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudMixin {
    @Shadow private ClientLevel level;
    @Shadow private int ticks;
    @Unique private final CirrusCloudRenderer cirrus$cloudRenderer = new CirrusCloudRenderer();
    @Unique private final CirrusAuroraRenderer cirrus$auroraRenderer = new CirrusAuroraRenderer();
    @Unique private final CirrusMilkyWayRenderer cirrus$milkyWayRenderer = new CirrusMilkyWayRenderer();
    @Unique private final CirrusLightningSkyRenderer cirrus$lightningSkyRenderer =
            new CirrusLightningSkyRenderer();
    @Unique private final CirrusEndSkyRenderer cirrus$endSkyRenderer = new CirrusEndSkyRenderer();
    @Unique private CloudStatus cirrus$lastCloudMode;
    @Unique private boolean cirrus$celestialMaskActive;
    @Unique private float cirrus$precipitationCeiling = Float.POSITIVE_INFINITY;

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
        CirrusCloudAttachment.updateRenderTicks(ticks);
        CloudStatus mode = Minecraft.getInstance().options.getCloudsType();
        if (mode != cirrus$lastCloudMode) {
            if (!CirrusCloudMode.isActive(mode)) {
                cirrus$cloudRenderer.invalidate();
            }
            cirrus$lastCloudMode = mode;
        }
    }

    @Redirect(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getCloudsType()Lnet/minecraft/client/CloudStatus;"
            )
    )
    private CloudStatus cirrus$keepCloudPassEnabled(Options options) {
        CloudStatus cloudStatus = options.getCloudsType();
        return CirrusCloudMode.isActive(cloudStatus) ? CloudStatus.FAST : cloudStatus;
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

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"))
    private void cirrus$preparePrecipitationCeiling(
            LightTexture lightTexture,
            float partialTick,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo ci
    ) {
        cirrus$precipitationCeiling = CirrusPrecipitationCeiling.relative(level, cameraY);
    }

    @ModifyArg(
            method = "renderSnowAndRain",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(FFF)"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            index = 1
    )
    private float cirrus$capPrecipitationAtHighestCloudLayer(float y) {
        return Math.min(y, cirrus$precipitationCeiling);
    }

    @Inject(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEndSky(" +
                            "Lcom/mojang/blaze3d/vertex/PoseStack;)V"
            ),
            cancellable = true
    )
    private void cirrus$renderEndSky(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable skyFogSetup,
            CallbackInfo ci
    ) {
        if (level == null
                || !Level.END.equals(level.dimension())
                || !CirrusConfig.END_SKY_ENABLED.get()) {
            return;
        }

        cirrus$endSkyRenderer.render(frustumMatrix, projectionMatrix, partialTick, ticks);
        ci.cancel();
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
            cirrus$lightningSkyRenderer.render(level, frustumMatrix, projectionMatrix, partialTick, camera);
            cirrus$auroraRenderer.render(level, frustumMatrix, projectionMatrix, partialTick, ticks, camera);
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
        CirrusLightningLocator.invalidate();
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void cirrus$releaseCloudsOnReload(ResourceManager resourceManager, CallbackInfo ci) {
        cirrus$cloudRenderer.invalidate();
        CirrusCloudAttachment.invalidate();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void cirrus$releaseCloudsOnShutdown(CallbackInfo ci) {
        cirrus$cloudRenderer.close();
        cirrus$auroraRenderer.close();
        cirrus$milkyWayRenderer.close();
        cirrus$lightningSkyRenderer.close();
        cirrus$endSkyRenderer.close();
        CirrusCloudAttachment.invalidate();
        CirrusLightningLocator.invalidate();
    }
}
