package com.jvn.cirrus.mixin.compat.wherewindsblow;

import com.jvn.cirrus.client.CirrusPrecipitationCeiling;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.jvn.wherewindsblow.client.weather.GpuSnowfallRenderer", remap = false)
public abstract class GpuSnowfallRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void cirrus$useCeilingAwareCpuSnowfall(
            ClientLevel level,
            float rainLevel,
            float thunder,
            double animationTime,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (Double.isFinite(CirrusPrecipitationCeiling.absolute(level))) {
            cir.setReturnValue(false);
        }
    }
}
