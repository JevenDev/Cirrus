package com.jvn.cirrus.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.timeline.Timeline;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.WorldClock;

public final class CirrusDayCycle {
    private static final double VANILLA_DAY_TICKS = 24000.0;

    private CirrusDayCycle() {
    }

    public static double dayTime(ClientLevel level, float partialTick) {
        Timeline timeline = daylightTimeline(level.dimensionType().timelines());
        double time = CirrusTimeTransition.visualDayTime(level, partialTick);
        if (timeline != null && !usesDefaultClock(level, timeline)) {
            time = clockTime(level.clockManager(), timeline.clock(), partialTick);
        }
        return normalize(time, periodTicks(timeline));
    }

    public static double periodTicks(ClientLevel level) {
        Timeline timeline = daylightTimeline(level.dimensionType().timelines());
        return timeline != null && usesDefaultClock(level, timeline) ? periodTicks(timeline) : VANILLA_DAY_TICKS;
    }

    private static boolean usesDefaultClock(ClientLevel level, Timeline timeline) {
        var timelineClock = timeline.clock();
        return level.dimensionType().defaultClock()
                .filter(clock -> clock.unwrapKey().map(timelineClock::is)
                        .orElseGet(() -> clock == timelineClock || clock.value() == timelineClock.value()))
                .isPresent();
    }

    static double clockTime(ClockManager clocks, Holder<WorldClock> clock, float partialTick) {
        var instance = clocks.getInstance(clock);
        return instance.totalTicks() + instance.partialTick() + partialTick * instance.rate();
    }

    static Timeline daylightTimeline(Iterable<Holder<Timeline>> timelines) {
        Timeline daylight = null;
        for (var holder : timelines) {
            if (holder.value().attributes().contains(EnvironmentAttributes.SUN_ANGLE)) {
                daylight = holder.value();
            }
        }
        return daylight;
    }

    static double periodTicks(Timeline timeline) {
        return timeline == null ? VANILLA_DAY_TICKS : timeline.periodTicks().orElse((int)VANILLA_DAY_TICKS);
    }

    static double normalize(double time, double periodTicks) {
        return time * (VANILLA_DAY_TICKS / periodTicks);
    }
}
