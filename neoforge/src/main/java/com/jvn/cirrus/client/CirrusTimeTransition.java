package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Smooths abrupt client-side day-time corrections for rendering without delaying
 * the authoritative time used by the rest of the client world.
 */
public final class CirrusTimeTransition {
    private static final long ABRUPT_TIME_CHANGE_TICKS = 40L;
    private static final double FULL_DAY_TICKS = 24000.0;
    private static final double FULL_NIGHT_TICKS = 12000.0;
    private static final double MIN_DURATION_TICKS = 40.0;
    private static final double MAX_DURATION_TICKS = 180.0;
    private static final double NANOS_PER_TICK = 50_000_000.0;

    private static ClientLevel trackedLevel;
    private static long lastActualDayTime;
    private static long lastGameTime;
    private static long visualDayTime;
    private static double displayedDayTime;
    private static double transitionStartDayTime;
    private static long transitionTargetDayTime;
    private static double transitionDelta;
    private static long transitionStartNanos;
    private static double transitionDurationTicks;
    private static double transitionProgress = 1.0;
    private static boolean transitionActive;
    private static boolean rendering;

    private CirrusTimeTransition() {
    }

    public static void beginFrame(ClientLevel level) {
        rendering = false;

        long actualDayTime = level.getLevelData().getDayTime();
        long gameTime = level.getGameTime();
        long now = System.nanoTime();
        if (!CirrusConfig.SMOOTH_TIME_TRANSITIONS.get() || trackedLevel != level) {
            reset(level, actualDayTime, gameTime);
        } else if (!level.dimensionType().hasFixedTime()) {
            long actualDelta = actualDayTime - lastActualDayTime;
            long gameTimeDelta = gameTime - lastGameTime;
            if (isAbruptChange(level, actualDelta, gameTimeDelta)) {
                updateTransition(lastActualDayTime, now);
                startTransition(actualDayTime, now);
            } else {
                updateTransition(actualDayTime, now);
            }
            lastActualDayTime = actualDayTime;
            lastGameTime = gameTime;
        } else {
            reset(level, actualDayTime, gameTime);
        }

        visualDayTime = Math.round(displayedDayTime);
        rendering = true;
    }

    public static void endFrame() {
        rendering = false;
    }

    public static boolean isRenderingWith(ClientLevel.ClientLevelData levelData) {
        return rendering
                && trackedLevel != null
                && trackedLevel.getLevelData() == levelData;
    }

    public static long visualDayTime() {
        return visualDayTime;
    }

    public static double visualDayTime(ClientLevel level, float partialTick) {
        float configuredRate = level.getDayTimePerTick();
        double ticksPerGameTick = configuredRate < 0.0F ? 1.0 : configuredRate;
        return displayedDayTime + level.getDayTimeFraction() + partialTick * ticksPerGameTick;
    }

    public static boolean isTransitionActive() {
        return transitionActive;
    }

    public static double transitionProgress() {
        return transitionProgress;
    }

    public static long authoritativeDayTime() {
        return lastActualDayTime;
    }

    private static void reset(ClientLevel level, long actualDayTime, long gameTime) {
        trackedLevel = level;
        lastActualDayTime = actualDayTime;
        lastGameTime = gameTime;
        visualDayTime = actualDayTime;
        displayedDayTime = actualDayTime;
        transitionStartDayTime = actualDayTime;
        transitionTargetDayTime = actualDayTime;
        transitionDelta = 0.0;
        transitionDurationTicks = 0.0;
        transitionProgress = 1.0;
        transitionActive = false;
    }

    private static boolean isAbruptChange(ClientLevel level, long actualDelta, long gameTimeDelta) {
        double expectedDelta = 0.0;
        if (gameTimeDelta > 0L) {
            float configuredRate = level.getDayTimePerTick();
            double ticksPerGameTick = configuredRate < 0.0F ? 1.0 : configuredRate;
            expectedDelta = gameTimeDelta * ticksPerGameTick;
        }
        return Math.abs(actualDelta - expectedDelta) > ABRUPT_TIME_CHANGE_TICKS;
    }

    private static void startTransition(long targetDayTime, long now) {
        transitionStartDayTime = displayedDayTime;
        transitionTargetDayTime = targetDayTime;
        transitionDelta = shortestDayDelta(displayedDayTime, targetDayTime);
        transitionDurationTicks = durationTicks(Math.abs(transitionDelta));
        transitionStartNanos = now;
        transitionProgress = 0.0;
        transitionActive = true;
    }

    private static double shortestDayDelta(double startDayTime, long targetDayTime) {
        double delta = (targetDayTime - startDayTime) % FULL_DAY_TICKS;
        if (delta > FULL_NIGHT_TICKS) {
            delta -= FULL_DAY_TICKS;
        } else if (delta < -FULL_NIGHT_TICKS) {
            delta += FULL_DAY_TICKS;
        }
        return delta;
    }

    private static void updateTransition(long actualDayTime, long now) {
        if (!transitionActive) {
            displayedDayTime = actualDayTime;
            transitionProgress = 1.0;
            return;
        }

        double elapsedTicks = Math.max(0.0, (now - transitionStartNanos) / NANOS_PER_TICK);
        double progress = transitionDurationTicks <= 0.0
                ? 1.0
                : Math.min(1.0, elapsedTicks / transitionDurationTicks);
        double easedProgress = smootherStep(progress);
        transitionProgress = easedProgress;
        double naturalTimePassed = actualDayTime - transitionTargetDayTime;
        displayedDayTime = transitionStartDayTime
                + transitionDelta * easedProgress
                + naturalTimePassed;
        if (progress >= 1.0) {
            displayedDayTime = actualDayTime;
            transitionProgress = 1.0;
            transitionActive = false;
        }
    }

    private static double durationTicks(double delta) {
        double nightFraction = Math.min(1.0, delta / FULL_NIGHT_TICKS);
        double baseDuration = MIN_DURATION_TICKS
                + (MAX_DURATION_TICKS - MIN_DURATION_TICKS) * nightFraction;
        return baseDuration / CirrusConfig.TIME_TRANSITION_SPEED.get();
    }

    private static double smootherStep(double value) {
        double x = Math.max(0.0, Math.min(1.0, value));
        return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
    }
}
