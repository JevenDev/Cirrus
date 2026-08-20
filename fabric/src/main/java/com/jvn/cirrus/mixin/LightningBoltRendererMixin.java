package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudAttachment;
import com.jvn.cirrus.client.CirrusLightningRenderStateAccess;
import com.jvn.cirrus.client.CirrusLightningRenderer;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightningBoltRenderer.class, priority = 900)
public abstract class LightningBoltRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cirrus$captureLightning(
            LightningBolt lightning,
            LightningBoltRenderState state,
            float partialTick,
            CallbackInfo ci
    ) {
        ((CirrusLightningRenderStateAccess)state).cirrus$setLightningData(
                lightning.tickCount + partialTick,
                CirrusCloudAttachment.findVisualOrigin(lightning, partialTick)
        );
    }

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void cirrus$renderBranchingLightning(
            LightningBoltRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState,
            CallbackInfo ci
    ) {
        if (!CirrusConfig.CUSTOM_LIGHTNING_ENABLED.get()) {
            return;
        }

        CirrusLightningRenderStateAccess access = (CirrusLightningRenderStateAccess)state;
        CirrusLightningRenderer.submit(
                state.seed,
                access.cirrus$age(),
                access.cirrus$visualOrigin(),
                poseStack,
                collector
        );
        ci.cancel();
    }
}
