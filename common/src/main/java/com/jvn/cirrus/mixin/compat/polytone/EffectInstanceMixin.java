package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.compat.polytone.SunbathingShaderCompat;
import com.jvn.cirrus.client.util.CirrusCelestialRenderState;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EffectInstance.class)
public abstract class EffectInstanceMixin {
    @Unique private int cirrus$sunDirectionLocation = -1;
    @Unique private int cirrus$moonDirectionLocation = -1;
    @Unique private final Vector3f cirrus$sunDirection = new Vector3f();
    @Unique private final Vector3f cirrus$moonDirection = new Vector3f();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cirrus$findCelestialUniforms(CallbackInfo ci) {
        EffectInstance effect = (EffectInstance)(Object)this;
        if (SunbathingShaderCompat.isSupported(effect.getName())) {
            cirrus$sunDirectionLocation = Uniform.glGetUniformLocation(effect.getId(), "CirrusSunDirection");
            cirrus$moonDirectionLocation = Uniform.glGetUniformLocation(effect.getId(), "CirrusMoonDirection");
        }
    }

    @Inject(method = "apply", at = @At("HEAD"))
    private void cirrus$prepareCelestialDirections(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (cirrus$sunDirectionLocation < 0 || minecraft.level == null) {
            return;
        }
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float timeOfDay = minecraft.level.getTimeOfDay(partialTick);
        CirrusCelestialRenderState.sunDirection(cirrus$sunDirection, timeOfDay, CirrusConfig.SUN_ANGLED_ORBIT.get());
        CirrusCelestialRenderState.moonDirection(cirrus$moonDirection, timeOfDay, CirrusConfig.MOON_ANGLED_ORBIT.get());
        Uniform angle = ((EffectInstance)(Object)this).getUniform("PolySunAngle");
        if (angle != null) {
            angle.set((float)-Math.asin(Math.clamp(cirrus$sunDirection.y, -1.0F, 1.0F)));
        }
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void cirrus$uploadCelestialDirections(CallbackInfo ci) {
        if (cirrus$sunDirectionLocation >= 0 && Minecraft.getInstance().level != null) {
            GL20.glUniform3f(cirrus$sunDirectionLocation,
                    cirrus$sunDirection.x, cirrus$sunDirection.y, cirrus$sunDirection.z);
            if (cirrus$moonDirectionLocation >= 0) {
                GL20.glUniform3f(cirrus$moonDirectionLocation,
                        cirrus$moonDirection.x, cirrus$moonDirection.y, cirrus$moonDirection.z);
            }
        }
    }
}
