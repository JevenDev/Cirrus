package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusClientClockAccess;
import com.jvn.cirrus.client.CirrusTimeTransition;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.ClientClockInstance.class)
public abstract class ClientClockInstanceMixin implements CirrusClientClockAccess {
    @Unique private Holder<WorldClock> cirrus$clock;

    @Override
    public void cirrus$setClock(Holder<WorldClock> clock) {
        cirrus$clock = clock;
    }

    @Inject(method = "totalTicks", at = @At("HEAD"), cancellable = true)
    private void cirrus$useTransitionTime(CallbackInfoReturnable<Long> cir) {
        if (cirrus$clock != null && CirrusTimeTransition.isRenderingWith(cirrus$clock)) {
            cir.setReturnValue(CirrusTimeTransition.visualDayTime());
        }
    }
}
