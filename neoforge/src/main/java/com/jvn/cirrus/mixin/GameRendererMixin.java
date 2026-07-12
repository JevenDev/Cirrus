package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.client.CirrusSky;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getDepthFar", at = @At("RETURN"), cancellable = true)
    private void cirrus$keepCloudsInsideProjection(CallbackInfoReturnable<Float> cir) {
        boolean cirrusCloudsActive = CirrusCloudMode.isActive(
                Minecraft.getInstance().options.getCloudsType()
        );
        cir.setReturnValue(Math.max(
                cir.getReturnValueF(),
                CirrusSky.minimumFarPlane(cirrusCloudsActive)
        ));
    }
}
