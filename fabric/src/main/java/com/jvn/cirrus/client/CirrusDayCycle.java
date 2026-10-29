package com.jvn.cirrus.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.timeline.Timeline;

public final class CirrusDayCycle {
    private static final double VANILLA_DAY_TICKS = 24000.0;

    private CirrusDayCycle() {
    }

    public static double dayTime(ClientLevel level, float partialTick) {
        Timeline timeline = daylightTimeline(level.dimensionType().timelines());
        double time = CirrusTimeTransition.visualDayTime(level, partialTick);
        return normalize(time, periodTicks(timeline));
    }

    public static double periodTicks(ClientLevel level) {
        Timeline timeline = daylightTimeline(level.dimensionType().timelines());
        return periodTicks(timeline);
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
