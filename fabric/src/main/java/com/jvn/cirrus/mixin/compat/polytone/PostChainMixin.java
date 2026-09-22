package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.compat.polytone.SunbathingShaderCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PostChain.class)
public abstract class PostChainMixin {
    @WrapOperation(method = "createPass", at = @At(
            value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline$Builder;build()Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
    ))
    private static RenderPipeline cirrus$celestialUniforms(
            RenderPipeline.Builder builder, Operation<RenderPipeline> original,
            TextureManager textures, PostChainConfig.Pass config, Identifier id
    ) {
        if (SunbathingShaderCompat.isSupported(config.fragmentShaderId().toString())) {
            builder.withUniform("CirrusCelestial", UniformType.UNIFORM_BUFFER);
        }
        return original.call(builder);
    }
}
