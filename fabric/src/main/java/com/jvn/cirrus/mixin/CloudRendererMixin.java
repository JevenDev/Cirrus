package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudAttachment;
import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.client.CirrusRenderers;
import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitRenderPassProvider;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void cirrus$releaseOnReload(
            Optional<CloudRenderer.TextureData> preparations,
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            CallbackInfo ci
    ) {
        CirrusRenderers.clouds().invalidate();
        CirrusCloudAttachment.invalidate();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/CloudStatus;Lcom/mojang/renderpearl/api/commands/RenderPass;)V",
            at = @At("HEAD"), cancellable = true
    )
    private void cirrus$renderClouds(CloudStatus status, RenderPass renderPass, CallbackInfo ci) {
        if (!cirrus$shouldRender()) {
            return;
        }
        ci.cancel();
        CirrusRenderContext.renderInPass(renderPass, null, this::cirrus$drawClouds);
    }

    @Inject(method = "renderOit", at = @At("HEAD"), cancellable = true)
    private void cirrus$renderCloudsOit(
            CloudStatus status, OitStage stage, GpuTextureView mainDepthTextureView,
            OitRenderPassProvider.Parameters parameters, CallbackInfo ci
    ) {
        if (!cirrus$shouldRender()) {
            return;
        }
        ci.cancel();
        try (RenderPass pass = OitRenderPassProvider.createRenderPass(stage, () -> "Cirrus clouds", parameters)) {
            if (stage == OitStage.DEPTH_BOUNDS) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.BLIT_DEPTH_DURING_DEPTH_BOUNDS));
                pass.setUniform("InSampler", mainDepthTextureView,
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                pass.draw(3, 1, 0, 0);
            }
            CirrusRenderContext.renderInPass(pass, stage, this::cirrus$drawClouds);
        }
    }

    @Unique
    private boolean cirrus$shouldRender() {
        return CirrusRenderContext.isReady()
                && CirrusCloudMode.isActive(Minecraft.getInstance().options.getCloudStatus());
    }

    @Unique
    private void cirrus$drawClouds() {
        if (CirrusRenderContext.cloudsRenderedIntoDistantHorizons()) {
            return;
        }
        Vec3 cameraPosition = CirrusRenderContext.camera().position();
        CirrusRenderers.clouds().render(
                CirrusRenderContext.level(),
                new PoseStack(),
                CirrusRenderContext.frustumMatrix(),
                CirrusRenderContext.projectionMatrix(),
                CirrusRenderContext.partialTick(),
                CirrusRenderContext.cloudTicks(),
                cameraPosition.x,
                cameraPosition.y,
                cameraPosition.z
        );
    }
}
