package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.cirrus.client.util.CirrusEasing;
import com.jvn.cirrus.client.util.CirrusColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * Samples the configurable sky palette at the visual world time. The fixed
 * phase boundaries follow Minecraft's daylight cycle, while the configured
 * transition duration controls how softly neighboring palettes cross-fade.
 */
public final class CirrusSkyPalette {
    private static final float DAY_TICKS = 24000.0F;
    private static final float DAY_START = 2000.0F;
    private static final float EVENING_START = 11500.0F;
    private static final float NIGHT_START = 14000.0F;
    private static final float MORNING_START = 23000.0F;

    private CirrusSkyPalette() {
    }

    public static Sample sample(ClientLevel level, float partialTick) {
        double visualTime = CirrusTimeTransition.visualDayTime(level, partialTick);
        float dayTick = (float)(visualTime % DAY_TICKS);
        if (dayTick < 0.0F) {
            dayTick += DAY_TICKS;
        }

        float duration = CirrusConfig.SKY_GRADIENT_TRANSITION_TICKS.get().floatValue();
        if (duration <= 0.0F) {
            return phaseAt(dayTick);
        }

        float halfDuration = duration * 0.5F;
        float wrappedMorningTick = dayTick;
        float wrappedMorningEnd = MORNING_START + halfDuration - DAY_TICKS;
        if (wrappedMorningTick < wrappedMorningEnd) {
            wrappedMorningTick += DAY_TICKS;
        }
        if (isWithin(wrappedMorningTick, MORNING_START, halfDuration)) {
            return blend(
                    samplePhase(Phase.NIGHT),
                    samplePhase(Phase.MORNING),
                    transitionAmount(wrappedMorningTick, MORNING_START, halfDuration)
            );
        }
        if (isWithin(dayTick, DAY_START, halfDuration)) {
            return blend(
                    samplePhase(Phase.MORNING),
                    samplePhase(Phase.DAY),
                    transitionAmount(dayTick, DAY_START, halfDuration)
            );
        }
        if (isWithin(dayTick, EVENING_START, halfDuration)) {
            return blend(
                    samplePhase(Phase.DAY),
                    samplePhase(Phase.EVENING),
                    transitionAmount(dayTick, EVENING_START, halfDuration)
            );
        }
        if (isWithin(dayTick, NIGHT_START, halfDuration)) {
            return blend(
                    samplePhase(Phase.EVENING),
                    samplePhase(Phase.NIGHT),
                    transitionAmount(dayTick, NIGHT_START, halfDuration)
            );
        }

        if (dayTick < DAY_START - halfDuration) {
            return samplePhase(Phase.MORNING);
        }
        if (dayTick < EVENING_START - halfDuration) {
            return samplePhase(Phase.DAY);
        }
        if (dayTick < NIGHT_START - halfDuration) {
            return samplePhase(Phase.EVENING);
        }
        if (dayTick < MORNING_START - halfDuration) {
            return samplePhase(Phase.NIGHT);
        }
        return samplePhase(Phase.MORNING);
    }

    private static Sample phaseAt(float dayTick) {
        if (dayTick < DAY_START || dayTick >= MORNING_START) {
            return samplePhase(Phase.MORNING);
        }
        if (dayTick < EVENING_START) {
            return samplePhase(Phase.DAY);
        }
        if (dayTick < NIGHT_START) {
            return samplePhase(Phase.EVENING);
        }
        return samplePhase(Phase.NIGHT);
    }

    private static Sample samplePhase(Phase phase) {
        return switch (phase) {
            case MORNING -> fromColors(
                    CirrusConfig.MORNING_HORIZON_COLOR.get(),
                    CirrusConfig.MORNING_ZENITH_COLOR.get(),
                    CirrusConfig.MORNING_GRADIENT_STRENGTH.get().floatValue()
            );
            case DAY -> fromColors(
                    CirrusConfig.DAY_HORIZON_COLOR.get(),
                    CirrusConfig.DAY_ZENITH_COLOR.get(),
                    CirrusConfig.DAY_GRADIENT_STRENGTH.get().floatValue()
            );
            case EVENING -> fromColors(
                    CirrusConfig.EVENING_HORIZON_COLOR.get(),
                    CirrusConfig.EVENING_ZENITH_COLOR.get(),
                    CirrusConfig.EVENING_GRADIENT_STRENGTH.get().floatValue()
            );
            case NIGHT -> fromColors(
                    CirrusConfig.NIGHT_HORIZON_COLOR.get(),
                    CirrusConfig.NIGHT_ZENITH_COLOR.get(),
                    CirrusConfig.NIGHT_GRADIENT_STRENGTH.get().floatValue()
            );
        };
    }

    private static Sample fromColors(int horizon, int zenith, float strength) {
        return new Sample(
                CirrusColors.red(horizon) / 255.0F,
                CirrusColors.green(horizon) / 255.0F,
                CirrusColors.blue(horizon) / 255.0F,
                CirrusColors.red(zenith) / 255.0F,
                CirrusColors.green(zenith) / 255.0F,
                CirrusColors.blue(zenith) / 255.0F,
                strength
        );
    }

    private static Sample blend(Sample from, Sample to, float amount) {
        return new Sample(
                Mth.lerp(amount, from.horizonRed, to.horizonRed),
                Mth.lerp(amount, from.horizonGreen, to.horizonGreen),
                Mth.lerp(amount, from.horizonBlue, to.horizonBlue),
                Mth.lerp(amount, from.zenithRed, to.zenithRed),
                Mth.lerp(amount, from.zenithGreen, to.zenithGreen),
                Mth.lerp(amount, from.zenithBlue, to.zenithBlue),
                Mth.lerp(amount, from.strength, to.strength)
        );
    }

    private static boolean isWithin(float tick, float center, float halfDuration) {
        return tick >= center - halfDuration && tick <= center + halfDuration;
    }

    private static float transitionAmount(float tick, float center, float halfDuration) {
        float linear = (tick - (center - halfDuration)) / (halfDuration * 2.0F);
        return CirrusEasing.smoothstep(linear);
    }

    private enum Phase {
        MORNING,
        DAY,
        EVENING,
        NIGHT
    }

    public record Sample(
            float horizonRed,
            float horizonGreen,
            float horizonBlue,
            float zenithRed,
            float zenithGreen,
            float zenithBlue,
            float strength
    ) {
    }
}
