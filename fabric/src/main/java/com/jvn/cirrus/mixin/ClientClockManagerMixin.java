package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusClientClockAccess;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin {
    @Inject(
            method = "getInstance(Lnet/minecraft/core/Holder;)Lnet/minecraft/client/ClientClockManager$ClientClockInstance;",
            at = @At("RETURN")
    )
    private void cirrus$identifyClock(
            Holder<WorldClock> clock,
            CallbackInfoReturnable<ClientClockManager.ClientClockInstance> cir
    ) {
        ((CirrusClientClockAccess)cir.getReturnValue()).cirrus$setClock(clock);
    }
}
