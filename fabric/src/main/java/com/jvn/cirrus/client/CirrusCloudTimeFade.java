package com.jvn.cirrus.client;

import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.jvn.cirrus.client.util.CirrusEasing;
import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

public final class CirrusCloudTimeFade {
    private static final double DAY_TICKS = 24000.0;
    private static final double HALF_TRANSITION = 1000.0;
    private static final double[] PHASE_TARGETS = {1000.0, 6000.0, 12000.0, 18000.0};
    private static final double[] PHASE_STARTS = {23000.0, 4000.0, 11000.0, 14000.0};

    private CirrusCloudTimeFade() {
    }

    public static float opacity(ClientLevel level, float partialTick) {
        return opacity(dayTime(level, partialTick));
    }

    static double dayTime(ClientLevel level, float partialTick) {
        return level.dimensionType().hasFixedTime()
                ? CirrusRenderContext.sunAngle(partialTick) / Mth.TWO_PI * DAY_TICKS
                : CirrusTimeTransition.canUseVisualTime(level)
                        ? CirrusTimeTransition.visualDayTime(level, partialTick)
                        : level.getDefaultClockTime() + partialTick;
    }

    public static float opacity(double dayTime) {
        double tick = (dayTime % DAY_TICKS + DAY_TICKS) % DAY_TICKS;
        for (int phase = 0; phase < PHASE_TARGETS.length; phase++) {
            int previous = (phase + 3) % 4;
            double start = PHASE_TARGETS[previous];
            double duration = (PHASE_TARGETS[phase] - start + DAY_TICKS) % DAY_TICKS;
            double elapsed = (tick - start + DAY_TICKS) % DAY_TICKS;
            if (elapsed <= duration) {
                double amount = elapsed / duration;
                if (transitionOnly(phase)) {
                    double transitionStart = (PHASE_STARTS[phase] - HALF_TRANSITION - start + DAY_TICKS) % DAY_TICKS;
                    amount = Math.clamp((elapsed - transitionStart) / (2.0 * HALF_TRANSITION), 0.0, 1.0);
                }
                float from = fade(previous);
                return 1.0F - (from + (fade(phase) - from) * CirrusEasing.smoothstep((float)amount));
            }
        }
        return 1.0F;
    }

    private static boolean transitionOnly(int phase) {
        return switch (phase) {
            case 0 -> CirrusConfig.DAY_CLOUD_FADE_TRANSITION_ONLY.get();
            case 1 -> CirrusConfig.NOON_CLOUD_FADE_TRANSITION_ONLY.get();
            case 2 -> CirrusConfig.EVENING_CLOUD_FADE_TRANSITION_ONLY.get();
            default -> CirrusConfig.NIGHT_CLOUD_FADE_TRANSITION_ONLY.get();
        };
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
