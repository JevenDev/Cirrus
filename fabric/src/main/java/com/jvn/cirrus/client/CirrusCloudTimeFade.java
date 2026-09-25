package com.jvn.cirrus.client;

import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.jvn.cirrus.client.util.CirrusEasing;
import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

public final class CirrusCloudTimeFade {
    private static final double DAY_TICKS = 24000.0;
    private static final double HALF_TRANSITION = 1000.0;
    private static final double[] PHASE_STARTS = {23000.0, 4000.0, 11000.0, 14000.0};

    private CirrusCloudTimeFade() {
    }

    public static float opacity(ClientLevel level, float partialTick) {
        double time = level.dimensionType().hasFixedTime()
                ? CirrusRenderContext.sunAngle(partialTick) / Mth.TWO_PI * DAY_TICKS
                : CirrusTimeTransition.visualDayTime(level, partialTick);
        return opacity(time);
    }

    public static float opacity(double dayTime) {
        double tick = (dayTime % DAY_TICKS + DAY_TICKS) % DAY_TICKS;
        for (int phase = 0; phase < PHASE_STARTS.length; phase++) {
            double offset = (tick - PHASE_STARTS[phase] + DAY_TICKS * 1.5) % DAY_TICKS - DAY_TICKS * 0.5;
            if (Math.abs(offset) <= HALF_TRANSITION) {
                float amount = CirrusEasing.smoothstep((float)((offset + HALF_TRANSITION) / (2.0 * HALF_TRANSITION)));
                float from = fade((phase + 3) % 4);
                return 1.0F - (from + (fade(phase) - from) * amount);
            }
        }
        int phase = tick < PHASE_STARTS[1] || tick >= PHASE_STARTS[0] ? 0
                : tick < PHASE_STARTS[2] ? 1 : tick < PHASE_STARTS[3] ? 2 : 3;
        return 1.0F - fade(phase);
    }

    private static float fade(int phase) {
        return switch (phase) {
            case 0 -> CirrusConfig.DAY_CLOUD_FADE.get().floatValue();
            case 1 -> CirrusConfig.NOON_CLOUD_FADE.get().floatValue();
            case 2 -> CirrusConfig.EVENING_CLOUD_FADE.get().floatValue();
            default -> CirrusConfig.NIGHT_CLOUD_FADE.get().floatValue();
        };
    }
}
