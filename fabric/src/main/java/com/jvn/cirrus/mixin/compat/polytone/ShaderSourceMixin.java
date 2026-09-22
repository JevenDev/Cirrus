package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.compat.polytone.SunbathingShaderCompat;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.shaders.ShaderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.ShaderManager$CompilationCache")
public abstract class ShaderSourceMixin {
    @Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true)
    private void cirrus$celestialDirections(Identifier id, ShaderType type, CallbackInfoReturnable<String> cir) {
        if (type == ShaderType.FRAGMENT && cir.getReturnValue() != null) {
            cir.setReturnValue(SunbathingShaderCompat.adapt(id.toString(), cir.getReturnValue()));
        }
    }
}
