package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusTimeTransition;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.ClientLevelData.class)
public abstract class ClientLevelDataMixin {
    @Inject(method = "getDayTime", at = @At("HEAD"), cancellable = true)
    private void cirrus$useTransitionTime(CallbackInfoReturnable<Long> cir) {
        ClientLevel.ClientLevelData levelData = (ClientLevel.ClientLevelData)(Object)this;
        if (CirrusTimeTransition.isRenderingWith(levelData)) {
            cir.setReturnValue(CirrusTimeTransition.visualDayTime());
        }
    }
}
