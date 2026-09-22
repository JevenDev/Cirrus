package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.jvn.cirrus.client.util.CirrusCelestialRenderState;
import com.jvn.cirrus.client.util.CirrusCelestialTransform;
import com.jvn.cirrus.config.CirrusConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererCelestialMixin {
    @Unique private final Matrix4f cirrus$fixedSky = new Matrix4f();
    @Unique private final Matrix4f cirrus$body = new Matrix4f();
    @Unique private final Matrix4f cirrus$sunrise = new Matrix4f();
    @Unique private float cirrus$sunAngle;
    @Unique private float cirrus$moonAngle;

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void cirrus$prepareOrbits(PoseStack poses, float timeOfDay, int moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci) {
        cirrus$fixedSky.set(poses.last().pose());
        cirrus$sunAngle = timeOfDay * Mth.TWO_PI;
        cirrus$moonAngle = cirrus$sunAngle;
    }

    @WrapMethod(method = "renderSun")
    private void cirrus$rotateSun(float brightness, PoseStack poses, Operation<Void> original) {
        poses.pushPose();
        try {
            if (CirrusRenderContext.isReady()) {
                if (CirrusConfig.SUN_ANGLED_ORBIT.get()) {
                    CirrusCelestialTransform.bodyModelView(
                            poses.last().pose(), cirrus$fixedSky, cirrus$sunAngle / Mth.TWO_PI, true
                    );
                }
                CirrusCelestialRenderState.captureSun(cirrus$body
                        .set(CirrusRenderContext.frustumMatrix()).mul(poses.last().pose()));
            }
            original.call(brightness, poses);
        } finally {
            poses.popPose();
        }
    }

    @WrapMethod(method = "renderMoon")
    private void cirrus$rotateMoon(int phase, float brightness, PoseStack poses, Operation<Void> original) {
        poses.pushPose();
        try {
            if (CirrusRenderContext.isReady()) {
                if (CirrusConfig.MOON_ANGLED_ORBIT.get()) {
                    CirrusCelestialTransform.bodyModelView(
                            poses.last().pose(), cirrus$fixedSky, cirrus$moonAngle / Mth.TWO_PI, true
                    );
                }
                CirrusCelestialRenderState.captureMoon(cirrus$body
                        .set(CirrusRenderContext.frustumMatrix()).mul(poses.last().pose()));
            }
            original.call(phase, brightness, poses);
        } finally {
            poses.popPose();
        }
    }

    @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"))
    private void cirrus$prepareSunrise(PoseStack poses, float angle, int color, CallbackInfo ci) {
        CirrusCelestialTransform.sunriseModelView(cirrus$sunrise, poses.last().pose(), angle / Mth.TWO_PI);
    }

    @ModifyArg(method = "renderSunriseAndSunset", at = @At(
            value = "INVOKE", target = "Lorg/joml/Matrix4fStack;mul(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;"
    ), index = 0)
    private Matrix4fc cirrus$rotateSunrise(Matrix4fc original) {
        return CirrusConfig.SUN_ANGLED_ORBIT.get() ? cirrus$sunrise : original;
    }
}
