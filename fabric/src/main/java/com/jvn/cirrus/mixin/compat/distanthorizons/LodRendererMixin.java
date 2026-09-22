package com.jvn.cirrus.mixin.compat.distanthorizons;

import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsApiCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.render.renderer.LodRenderer", remap = false)
public abstract class LodRendererMixin {
    @WrapOperation(method = "renderTerrain", at = @At(
            value = "INVOKE",
            target = "Lcom/seibel/distanthorizons/core/wrapperInterfaces/render/renderPass/IDhAntiAliasRenderer;"
                    + "render(Lcom/seibel/distanthorizons/core/render/RenderParams;)V"
    ))
    private void cirrus$renderCloudsBeforeAntiAliasing(
            @Coerce Object renderer,
            @Coerce DhApiRenderParam params,
            Operation<Void> original
    ) {
        // fill the sun mask before DH's temporal filter can spread its edges into the sky
        DistantHorizonsApiCompat.renderClouds(params);
        original.call(renderer, params);
    }
}
