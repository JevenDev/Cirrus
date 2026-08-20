package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.client.CirrusRenderers;
import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cirrus$renderClouds(
            int color,
            CloudStatus status,
            float cloudHeight,
            int cloudRange,
            Vec3 cameraPosition,
            long ticks,
            float partialTick,
            CallbackInfo ci
    ) {
        if (!CirrusRenderContext.isReady()
                || !CirrusCloudMode.isActive(Minecraft.getInstance().options.getCloudStatus())) {
            return;
        }

        ci.cancel();
        if (CirrusRenderContext.cloudsRenderedIntoDistantHorizons()) {
            return;
        }
        CirrusRenderers.clouds().render(
                CirrusRenderContext.level(),
                new PoseStack(),
                CirrusRenderContext.frustumMatrix(),
                CirrusRenderContext.projectionMatrix(),
                partialTick,
                (int)ticks,
                cameraPosition.x,
                cameraPosition.y,
                cameraPosition.z
        );
    }
}
