package com.jvn.cirrus.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.multiplayer.ClientLevel;

public final class CirrusTimeAccess {
    private CirrusTimeAccess() {
    }

    @ExpectPlatform
    public static float dayTimeFraction(ClientLevel level) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static float dayTimePerTick(ClientLevel level) {
        throw new AssertionError();
    }
}
