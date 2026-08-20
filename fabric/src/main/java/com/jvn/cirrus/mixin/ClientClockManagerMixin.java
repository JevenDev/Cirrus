package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusClientClockAccess;
import com.jvn.cirrus.client.CirrusTimeTransition;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientClockManager.class)
public abstract class ClientClockManagerMixin implements CirrusClientClockAccess {
    @Unique private final Map<Holder<WorldClock>, Float> cirrus$rates = new HashMap<>();

    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void cirrus$useTransitionTime(
            Holder<WorldClock> clock,
            CallbackInfoReturnable<Long> cir
    ) {
        if (CirrusTimeTransition.isRenderingWith(clock)) {
            cir.setReturnValue(CirrusTimeTransition.visualDayTime());
        }
    }

    @Inject(method = "handleUpdates", at = @At("TAIL"))
    private void cirrus$captureRates(
            long gameTime,
            Map<Holder<WorldClock>, ClockNetworkState> updates,
            CallbackInfo ci
    ) {
        updates.forEach((clock, state) -> cirrus$rates.put(clock, state.rate()));
    }

    @Override
    public float cirrus$rate(Holder<WorldClock> clock) {
        return cirrus$rates.getOrDefault(clock, 1.0F);
    }
}
