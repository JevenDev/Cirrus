package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.compat.polytone.SunbathingShaderCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.class)
public abstract class ShaderSourceMixin {
    @ModifyExpressionValue(method = "loadShader", at = @At(
            value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"
    ))
    private static String cirrus$celestialDirections(String source, Identifier location, Resource resource, ShaderType type) {
        return type == ShaderType.FRAGMENT
                ? SunbathingShaderCompat.adapt(type.idConverter().fileToId(location).toString(), source) : source;
    }
}
