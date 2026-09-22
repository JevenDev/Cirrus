package com.jvn.cirrus.mixin.compat.caelum;

import com.jvn.cirrus.client.CirrusSunMask;
import com.jvn.cirrus.client.util.CirrusCelestialRenderState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.sophka.caelum.client.CelestialRenderer", remap = false)
public abstract class CelestialRendererMixin {
    @Unique private static boolean cirrus$renderingSun;

    @Inject(method = "renderObjects", at = @At("RETURN"))
    private static void cirrus$captureMoon(CallbackInfo ci) {
        if (CirrusCelestialRenderState.hasExternalSun()) {
            CirrusCelestialRenderState.captureExternalMoon(ClientSkyUtilsAccessor.cirrus$moonDirection());
        }
    }

    @WrapMethod(method = "renderSun")
    private static void cirrus$trackSun(PoseStack poses, float partialTick, Operation<Void> original) {
        cirrus$renderingSun = true;
        try {
            original.call(poses, partialTick);
        } finally {
            cirrus$renderingSun = false;
        }
    }

    @ModifyArg(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex("
                            + "Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
                    remap = true
            ),
            index = 0
    )
    private static Matrix4f cirrus$captureSun(Matrix4f modelView) {
        if (cirrus$renderingSun) {
            CirrusCelestialRenderState.captureExternalSun(modelView);
        }
        return modelView;
    }

    @WrapOperation(
            method = "*",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader("
                            + "Lcom/mojang/blaze3d/vertex/MeshData;)V",
                    remap = true
            )
    )
    private static void cirrus$maskSun(MeshData mesh, Operation<Void> original) {
        CirrusSunMask mask = (CirrusSunMask)Minecraft.getInstance().levelRenderer;
        ShaderInstance shader = RenderSystem.getShader();
        boolean masked = cirrus$renderingSun && mask.cirrus$beginSunMask();
        try {
            original.call(mesh);
        } finally {
            if (masked) {
                mask.cirrus$endSunMask();
                RenderSystem.setShader(() -> shader);
            }
        }
    }
}
