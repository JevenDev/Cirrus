package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusSky;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.environment.AirBasedFogEnvironment;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AirBasedFogEnvironment.class)
public abstract class AirBasedFogEnvironmentMixin {
    @WrapOperation(method = "getBaseColor", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;getLookVector()Lorg/joml/Vector3f;"
    ))
    private Vector3f cirrus$useHorizonViewDirection(Camera camera, Operation<Vector3f> original) {
        return CirrusSky.matchesFogColor(camera, Minecraft.getInstance().level)
                ? CirrusSky.horizonDirection(camera) : original.call(camera);
    }
}
