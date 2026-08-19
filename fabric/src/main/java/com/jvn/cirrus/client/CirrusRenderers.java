package com.jvn.cirrus.client;

public final class CirrusRenderers {
    private static final CirrusCloudRenderer CLOUDS = new CirrusCloudRenderer();
    private static final CirrusAuroraRenderer AURORA = new CirrusAuroraRenderer();
    private static final CirrusMilkyWayRenderer MILKY_WAY = new CirrusMilkyWayRenderer();
    private static final CirrusLightningSkyRenderer LIGHTNING_SKY = new CirrusLightningSkyRenderer();
    private static final CirrusEndSkyRenderer END_SKY = new CirrusEndSkyRenderer();
    private static final CirrusStarRenderer STARS = new CirrusStarRenderer();

    private CirrusRenderers() {
    }

    public static CirrusCloudRenderer clouds() {
        return CLOUDS;
    }

    public static CirrusAuroraRenderer aurora() {
        return AURORA;
    }

    public static CirrusMilkyWayRenderer milkyWay() {
        return MILKY_WAY;
    }

    public static CirrusLightningSkyRenderer lightningSky() {
        return LIGHTNING_SKY;
    }

    public static CirrusEndSkyRenderer endSky() {
        return END_SKY;
    }

    public static CirrusStarRenderer stars() {
        return STARS;
    }

    public static void closeSky() {
        AURORA.close();
        MILKY_WAY.close();
        LIGHTNING_SKY.close();
        END_SKY.close();
        STARS.close();
    }
}
