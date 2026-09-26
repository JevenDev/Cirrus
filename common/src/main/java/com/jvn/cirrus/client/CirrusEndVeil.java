package com.jvn.cirrus.client;

public final class CirrusEndVeil {
    private static final float FADE_TICKS = 30.0F;
    private static final float SHATTER_TICKS = 40.0F;
    private static final float RUPTURE_DEATH_TICK = 180.0F;

    private boolean initialized;
    private boolean bossFog;
    private float deathStart = Float.NaN;
    private float fadeStart;
    private float fadeFrom;
    private float fadeTo;
    private float shatterStart = Float.NaN;

    public void update(boolean fog, int dragonDeathTicks, int ticks) {
        if (!initialized) {
            initialized = true;
            bossFog = fog;
            fadeFrom = fadeTo = fog ? 1.0F : 0.0F;
            fadeStart = ticks;
        }
        if (fog != bossFog) {
            float currentOpacity = opacity(ticks);
            if (fog) {
                shatterStart = Float.NaN;
                deathStart = Float.NaN;
                fadeFrom = currentOpacity;
                fadeTo = 1.0F;
            } else if (!Float.isNaN(deathStart)) {
                fadeFrom = fadeTo = currentOpacity;
            } else {
                // leaving the boss bar's range is not a dragon defeat
                fadeFrom = currentOpacity;
                fadeTo = 0.0F;
            }
            fadeStart = ticks;
            bossFog = fog;
        }
        if (fog && dragonDeathTicks > 0 && Float.isNaN(deathStart)) {
            deathStart = ticks - dragonDeathTicks;
            shatterStart = deathStart + RUPTURE_DEATH_TICK;
        }
    }

    public float opacity(float ticks) {
        if (shatter(ticks) >= 1.0F) {
            return 0.0F;
        }
        float progress = Math.clamp((ticks - fadeStart) / FADE_TICKS, 0.0F, 1.0F);
        progress = progress * progress * (3.0F - 2.0F * progress);
        return fadeFrom + (fadeTo - fadeFrom) * progress;
    }

    public float charge(float ticks) {
        return Float.isNaN(deathStart)
                ? 0.0F
                : Math.clamp((ticks - deathStart) / RUPTURE_DEATH_TICK, 0.0F, 1.0F);
    }

    public float flash(float ticks) {
        float progress = shatter(ticks);
        float rise = Math.clamp(progress / 0.035F, 0.0F, 1.0F);
        float fall = Math.clamp((progress - 0.25F) / 0.18F, 0.0F, 1.0F);
        rise = rise * rise * (3.0F - 2.0F * rise);
        fall = fall * fall * (3.0F - 2.0F * fall);
        return rise * (1.0F - fall) * 0.75F;
    }

    public float shatter(float ticks) {
        return Float.isNaN(shatterStart)
                ? 0.0F
                : Math.clamp((ticks - shatterStart) / SHATTER_TICKS, 0.0F, 1.0F);
    }
}
