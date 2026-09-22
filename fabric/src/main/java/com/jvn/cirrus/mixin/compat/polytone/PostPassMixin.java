package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.compat.polytone.SunbathingShaderCompat;
import com.jvn.cirrus.client.compat.polytone.SunbathingLegacyPostCompat;
import com.jvn.cirrus.client.util.CirrusCelestialRenderState;
import com.jvn.cirrus.config.CirrusConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Map;
import net.minecraft.client.renderer.PostPass;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostPass.class)
public abstract class PostPassMixin {
    @Shadow @Final private RenderPipeline pipeline;
    @Shadow @Final private Map<String, GpuBuffer> customUniforms;
    @Unique private GpuBuffer cirrus$celestialUniforms;
    @Unique private GpuBuffer cirrus$legacyGlobals;
    @Unique private final Vector3f cirrus$sun = new Vector3f();
    @Unique private final Vector3f cirrus$moon = new Vector3f();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cirrus$createCelestialUniforms(CallbackInfo ci) {
        if (SunbathingShaderCompat.isSupported(pipeline.getFragmentShader().toString())) {
            cirrus$celestialUniforms = RenderSystem.getDevice().createBuffer(
                    () -> "Cirrus celestial directions", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32
            );
            customUniforms.put("CirrusCelestial", cirrus$celestialUniforms);
            if (SunbathingLegacyPostCompat.needsBridge()) {
                cirrus$legacyGlobals = RenderSystem.getDevice().createBuffer(
                        () -> "Sunbathing globals", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 144
                );
                customUniforms.put("PolyGlobals", cirrus$legacyGlobals);
            }
        }
    }

    @WrapOperation(method = "addToFrame", at = @At(
            value = "INVOKE", target = "Lcom/mojang/blaze3d/framegraph/FramePass;executes(Ljava/lang/Runnable;)V"
    ))
    private void cirrus$updateBeforePassExecution(FramePass pass, Runnable render, Operation<Void> original) {
        if (cirrus$celestialUniforms == null) {
            original.call(pass, render);
            return;
        }
        original.call(pass, (Runnable)() -> {
            cirrus$updateCelestialUniforms();
            render.run();
        });
    }

    @Unique
    private void cirrus$updateCelestialUniforms() {
        CirrusCelestialRenderState.sunDirection(cirrus$sun, 0.0F, CirrusConfig.SUN_ANGLED_ORBIT.get());
        CirrusCelestialRenderState.moonDirection(cirrus$moon, 0.0F, CirrusConfig.MOON_ANGLED_ORBIT.get());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (cirrus$legacyGlobals != null) {
                Std140Builder globals = Std140Builder.onStack(stack, 144);
                SunbathingLegacyPostCompat.writeGlobals(globals);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(cirrus$legacyGlobals.slice(), globals.get());
            }
            Std140Builder builder = Std140Builder.onStack(stack, 32);
            builder.putVec4(cirrus$sun.x, cirrus$sun.y, cirrus$sun.z, 0.0F);
            builder.putVec4(cirrus$moon.x, cirrus$moon.y, cirrus$moon.z, 0.0F);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(cirrus$celestialUniforms.slice(), builder.get());
        }
    }
}
