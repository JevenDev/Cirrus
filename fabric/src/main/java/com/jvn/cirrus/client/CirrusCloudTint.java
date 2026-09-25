package com.jvn.cirrus.client;

import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.jvn.cirrus.client.util.CirrusEasing;
import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public final class CirrusCloudTint {
    private static final double DAY_TICKS = 24000.0;
    private static final double[] PHASE_TARGETS = {1000.0, 6000.0, 12000.0, 18000.0};

    private CirrusCloudTint() {
    }

    public static Vec3 color(ClientLevel level, float partialTick) {
        Vec3 color = CirrusRenderContext.cloudColor(partialTick)
                .multiply(fromRgb(CirrusCloudDimensions.tint(level.dimension().identifier())));
        return CirrusConfig.CLOUD_TIME_TINT_ENABLED.get()
                ? color.multiply(timeTint(CirrusCloudTimeFade.dayTime(level, partialTick))) : color;
    }

    public static Vec3 timeTint(double dayTime) {
        double tick = (dayTime % DAY_TICKS + DAY_TICKS) % DAY_TICKS;
        for (int phase = 0; phase < PHASE_TARGETS.length; phase++) {
            int previous = (phase + 3) % 4;
            double start = PHASE_TARGETS[previous];
            double duration = (PHASE_TARGETS[phase] - start + DAY_TICKS) % DAY_TICKS;
            double elapsed = (tick - start + DAY_TICKS) % DAY_TICKS;
            if (elapsed <= duration) {
                float amount = CirrusEasing.smoothstep((float)(elapsed / duration));
                return fromRgb(tint(previous)).lerp(fromRgb(tint(phase)), amount);
            }
        }
        return new Vec3(1.0, 1.0, 1.0);
    }

    private static int tint(int phase) {
        return switch (phase) {
            case 0 -> CirrusConfig.DAY_CLOUD_TINT.get();
            case 1 -> CirrusConfig.NOON_CLOUD_TINT.get();
            case 2 -> CirrusConfig.EVENING_CLOUD_TINT.get();
            default -> CirrusConfig.NIGHT_CLOUD_TINT.get();
        };
    }

    private static Vec3 fromRgb(int color) {
        return new Vec3(
                ARGB.red(color) / 255.0,
                ARGB.green(color) / 255.0,
                ARGB.blue(color) / 255.0
        );
    }
}
