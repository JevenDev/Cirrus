package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusTimeTransition;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setDayTime", at = @At("TAIL"))
    private void cirrus$acceptInitialDayTime(long dayTime, CallbackInfo ci) {
        CirrusTimeTransition.acceptInitialTime((ClientLevel)(Object)this);
    }
}
