package com.jvn.cirrus.platform;

import com.jvn.cirrus.client.CirrusClientClockAccess;
import net.minecraft.client.multiplayer.ClientLevel;

public final class CirrusTimeAccess {
    private CirrusTimeAccess() {
    }

    public static float dayTimeFraction(ClientLevel level) {
        return 0.0F;
    }

    public static float dayTimePerTick(ClientLevel level) {
        return level.dimensionType().defaultClock()
                .map(clock -> ((CirrusClientClockAccess)level.clockManager()).cirrus$rate(clock))
                .orElse(0.0F);
    }
}
