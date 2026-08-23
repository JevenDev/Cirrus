package com.jvn.cirrus.fabric.mixin;

import com.jvn.cirrus.client.CirrusStarRenderer;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererStarMixin {
    @Shadow private ClientLevel level;
    @Shadow private int ticks;
    @Unique private final CirrusStarRenderer cirrus$starRenderer = new CirrusStarRenderer();

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(" +
                            "Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;" +
                            "Lnet/minecraft/client/renderer/ShaderInstance;)V",
                    ordinal = 1
            )
    )
    private void cirrus$renderShaderStars(
            VertexBuffer vanillaStars,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            ShaderInstance vanillaShader,
            Matrix4f frustumMatrix,
            Matrix4f renderSkyProjectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable skyFogSetup
    ) {
        boolean renderStarField = CirrusConfig.CUSTOM_STARS_ENABLED.get();
        boolean renderShootingStars = CirrusConfig.SHOOTING_STARS_ENABLED.get();
        if (level != null && (renderStarField || renderShootingStars)) {
            if (!renderStarField) {
                vanillaStars.drawWithShader(modelViewMatrix, projectionMatrix, vanillaShader);
            }
            cirrus$starRenderer.render(
                    level,
                    projectionMatrix,
                    frustumMatrix,
                    partialTick,
                    ticks,
                    renderStarField
            );
        } else {
            vanillaStars.drawWithShader(modelViewMatrix, projectionMatrix, vanillaShader);
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void cirrus$releaseStarsOnShutdown(CallbackInfo ci) {
        cirrus$starRenderer.close();
    }
}
