package com.jvn.cirrus.platform;

import net.minecraft.client.multiplayer.ClientLevel;

public final class CirrusTimeAccess {
    private CirrusTimeAccess() {
    }

    public static float dayTimeFraction(ClientLevel level) {
        return level.dimensionType().defaultClock()
                .map(clock -> level.clockManager().getInstance(clock).partialTick())
                .orElse(0.0F);
    }

    public static float dayTimePerTick(ClientLevel level) {
        return level.dimensionType().defaultClock()
                .map(clock -> level.clockManager().getInstance(clock).rate())
                .orElse(0.0F);
    }
}
