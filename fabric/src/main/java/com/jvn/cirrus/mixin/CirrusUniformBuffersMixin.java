package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusShaders;
import net.minecraft.client.renderer.DynamicUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicUniforms.class)
public abstract class CirrusUniformBuffersMixin {
    @Inject(method = "reset", at = @At("RETURN"))
    private void cirrus$endFrame(CallbackInfo ci) {
        CirrusShaders.endFrame();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void cirrus$closeBuffers(CallbackInfo ci) {
        CirrusShaders.closeBuffers();
    }
}
