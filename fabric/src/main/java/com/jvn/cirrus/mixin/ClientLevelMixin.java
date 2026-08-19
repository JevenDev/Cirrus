package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusClientLevelAccess;
import com.jvn.cirrus.client.CirrusTimeTransition;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin implements CirrusClientLevelAccess {
    @Shadow private boolean tickDayTime;

    @Override
    public boolean cirrus$tickDayTime() {
        return tickDayTime;
    }
    @Inject(method = "setTimeFromServer", at = @At("TAIL"))
    private void cirrus$acceptInitialDayTime(long gameTime, long dayTime, boolean tickDayTime, CallbackInfo ci) {
        CirrusTimeTransition.acceptInitialTime((ClientLevel)(Object)this);
    }
}
