package com.jvn.cirrus.platform;

import net.minecraft.client.multiplayer.ClientLevel;
import com.jvn.cirrus.client.CirrusClientLevelAccess;

public final class CirrusTimeAccess {
    private CirrusTimeAccess() {
    }

    public static float dayTimeFraction(ClientLevel level) {
        return 0.0F;
    }

    public static float dayTimePerTick(ClientLevel level) {
        return ((CirrusClientLevelAccess)level).cirrus$tickDayTime() ? 1.0F : 0.0F;
    }
}
