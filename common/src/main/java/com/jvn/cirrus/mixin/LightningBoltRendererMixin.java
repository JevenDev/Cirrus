package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusLightningRenderer;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightningBoltRenderer.class, priority = 900)
public abstract class LightningBoltRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/world/entity/LightningBolt;FF"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cirrus$renderBranchingLightning(
            LightningBolt lightning,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!CirrusConfig.CUSTOM_LIGHTNING_ENABLED.get()) {
            return;
        }
        CirrusLightningRenderer.render(lightning, partialTick, poseStack, buffers);
        ci.cancel();
    }
}
