package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.compat.polytone.SunbathingLegacyPostCompat;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
public abstract class ShaderReloadMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void cirrus$reloadSunbathing(CallbackInfo ci) {
        SunbathingLegacyPostCompat.reload();
    }
}
