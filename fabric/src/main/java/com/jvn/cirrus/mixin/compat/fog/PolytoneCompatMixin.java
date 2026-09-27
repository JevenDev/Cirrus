package com.jvn.cirrus.mixin.compat.fog;

import com.jvn.cirrus.client.compat.fog.FogModCompat;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.imb11.fog.client.compat.polytone.PolytoneCompat", remap = false)
public abstract class PolytoneCompatMixin {
    @ModifyReturnValue(method = "shouldUsePolytone", at = @At("RETURN"), remap = false, require = 0)
    private static boolean cirrus$skipIncompatiblePolytoneFog(boolean original) {
        return original && FogModCompat.supportsPolytoneFogIntegration();
    }
}
