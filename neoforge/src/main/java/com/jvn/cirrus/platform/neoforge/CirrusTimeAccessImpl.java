package com.jvn.cirrus.platform.neoforge;

import net.minecraft.client.multiplayer.ClientLevel;

public final class CirrusTimeAccessImpl {
    private CirrusTimeAccessImpl() {
    }

    public static float dayTimeFraction(ClientLevel level) {
        return level.getDayTimeFraction();
    }

    public static float dayTimePerTick(ClientLevel level) {
        return level.getDayTimePerTick();
    }
}
