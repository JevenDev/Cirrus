package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.client.CirrusRenderers;
import com.jvn.cirrus.client.CirrusShaders;
import com.jvn.cirrus.client.compat.shaderpacks.CirrusShaderPackCompat;
import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SkyRenderer.class, priority = 900)
public abstract class SkyRendererMixin {
    @Unique private boolean cirrus$sunMaskActive;

    @WrapMethod(method = "renderSunMoonAndStars")
    private void cirrus$useSkyPass(
            RenderPass renderPass, PoseStack poseStack, float sunAngle, float moonAngle,
            float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness,
            Operation<Void> original
    ) {
        CirrusRenderContext.renderInPass(renderPass, null, () -> original.call(
                renderPass, poseStack, sunAngle, moonAngle, starAngle, moonPhase, rainBrightness, starBrightness
        ));
    }

    @WrapMethod(method = "renderEndSky")
    private void cirrus$useEndPass(RenderPass renderPass, Operation<Void> original) {
        CirrusRenderContext.renderInPass(renderPass, null, () -> original.call(renderPass));
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void cirrus$renderMilkyWay(
            RenderPass renderPass,
            PoseStack poseStack,
            float sunAngle,
            float moonAngle,
            float starAngle,
            MoonPhase moonPhase,
            float rainBrightness,
            float starBrightness,
            CallbackInfo ci
    ) {
        if (CirrusRenderContext.isReady()) {
            CirrusRenderers.milkyWay().render(
                    CirrusRenderContext.level(),
                    CirrusRenderContext.frustumMatrix(),
                    CirrusRenderContext.projectionMatrix(),
                    CirrusRenderContext.partialTick()
            );
        }
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
    private void cirrus$renderAuroraAndLightning(
            RenderPass renderPass,
            PoseStack poseStack,
            float sunAngle,
            float moonAngle,
            float starAngle,
            MoonPhase moonPhase,
            float rainBrightness,
            float starBrightness,
            CallbackInfo ci
    ) {
        if (!CirrusRenderContext.isReady()) {
            return;
        }
        CirrusRenderers.lightningSky().render(
                CirrusRenderContext.level(),
                CirrusRenderContext.frustumMatrix(),
                CirrusRenderContext.projectionMatrix(),
                CirrusRenderContext.partialTick(),
                CirrusRenderContext.camera()
        );
        CirrusRenderers.aurora().render(
                CirrusRenderContext.level(),
                CirrusRenderContext.frustumMatrix(),
                CirrusRenderContext.projectionMatrix(),
                CirrusRenderContext.partialTick(),
                CirrusRenderContext.ticks(),
                CirrusRenderContext.camera()
        );
    }

    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void cirrus$renderStars(RenderPass renderPass, float brightness, PoseStack poseStack, CallbackInfo ci) {
        if (!CirrusRenderContext.isReady()) {
            return;
        }
        boolean renderStarField = CirrusConfig.CUSTOM_STARS_ENABLED.get();
        boolean renderShootingStars = CirrusConfig.SHOOTING_STARS_ENABLED.get();
        if (!renderStarField && !renderShootingStars) {
            return;
        }

        CirrusRenderers.stars().render(
                CirrusRenderContext.level(),
                CirrusRenderContext.projectionMatrix(),
                CirrusRenderContext.frustumMatrix(),
                CirrusRenderContext.partialTick(),
                CirrusRenderContext.ticks(),
                renderStarField
        );
        if (renderStarField) {
            ci.cancel();
        }
    }

    @Inject(method = "renderEndSky", at = @At("HEAD"), cancellable = true)
    private void cirrus$renderEndSky(RenderPass renderPass, CallbackInfo ci) {
        if (!CirrusRenderContext.isReady()
                || !Level.END.equals(CirrusRenderContext.level().dimension())
                || !CirrusConfig.END_SKY_ENABLED.get()) {
            return;
        }

        CirrusRenderers.endSky().render(
                CirrusRenderContext.frustumMatrix(),
                CirrusRenderContext.projectionMatrix(),
                CirrusRenderContext.partialTick(),
                CirrusRenderContext.ticks()
        );
        ci.cancel();
    }

    @Inject(method = "renderSun", at = @At("HEAD"))
    private void cirrus$maskSun(RenderPass renderPass, float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (!CirrusRenderContext.isReady()
                || CirrusShaderPackCompat.isShaderPackInUse()
                || !CirrusCloudMode.isActive(Minecraft.getInstance().options.getCloudStatus())) {
            return;
        }

        cirrus$sunMaskActive = CirrusRenderers.clouds().renderSunMask(
                CirrusRenderContext.level(),
                CirrusRenderContext.frustumMatrix(),
                CirrusRenderContext.projectionMatrix(),
                CirrusRenderContext.partialTick(),
                CirrusRenderContext.cloudTicks(),
                CirrusRenderContext.camera().position().x,
                CirrusRenderContext.camera().position().y,
                CirrusRenderContext.camera().position().z
        );
    }

    @Inject(method = "renderSun", at = @At("TAIL"))
    private void cirrus$clearSunMask(RenderPass renderPass, float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (cirrus$sunMaskActive) {
            CirrusShaders.clearSunMask(renderPass);
            cirrus$sunMaskActive = false;
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void cirrus$releaseSky(CallbackInfo ci) {
        CirrusRenderers.closeSky();
    }
}
