package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusSky;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getDepthFar", at = @At("RETURN"), cancellable = true)
    private void cirrus$keepCloudsInsideProjection(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValueF(), CirrusSky.minimumFarPlane()));
    }
}
