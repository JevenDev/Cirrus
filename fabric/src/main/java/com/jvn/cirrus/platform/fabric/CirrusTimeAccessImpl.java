package com.jvn.cirrus.platform.fabric;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.GameRules;

public final class CirrusTimeAccessImpl {
    private CirrusTimeAccessImpl() {
    }

    public static float dayTimeFraction(ClientLevel level) {
        return 0.0F;
    }

    public static float dayTimePerTick(ClientLevel level) {
        return level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) ? 1.0F : 0.0F;
    }
}
