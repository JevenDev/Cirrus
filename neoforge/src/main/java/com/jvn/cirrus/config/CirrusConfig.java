package com.jvn.cirrus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CirrusConfig {
    public static final ModConfigSpec.IntValue CLOUD_RENDER_DISTANCE;
    public static final ModConfigSpec.EnumValue<CloudQuality> LOWER_LAYER_QUALITY;
    public static final ModConfigSpec.DoubleValue LOWER_LAYER_HEIGHT_OFFSET;
    public static final ModConfigSpec.DoubleValue LOWER_LAYER_SPEED;
    public static final ModConfigSpec.DoubleValue LOWER_LAYER_OPACITY;
    public static final ModConfigSpec.BooleanValue UPPER_LAYER_ENABLED;
    public static final ModConfigSpec.EnumValue<CloudQuality> UPPER_LAYER_QUALITY;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_HEIGHT_OFFSET;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_SPEED;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_OPACITY;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Cloud rendering").push("clouds");
        CLOUD_RENDER_DISTANCE = builder
                .comment("Cloud render distance in chunks, independent of terrain render distance.")
                .defineInRange("renderDistanceChunks", 128, 2, 128);
        LOWER_LAYER_QUALITY = builder
                .comment("Geometry quality for the normal cloud layer.")
                .defineEnum("lowerLayerQuality", CloudQuality.FANCY);
        LOWER_LAYER_HEIGHT_OFFSET = builder
                .comment("Normal cloud layer height offset from the dimension's cloud height, in blocks.")
                .defineInRange("lowerLayerHeightOffset", 0.0, -128.0, 128.0);
        LOWER_LAYER_SPEED = builder
                .comment("Normal cloud layer speed multiplier. Zero makes the layer stationary.")
                .defineInRange("lowerLayerSpeed", 1.0, 0.0, 4.0);
        LOWER_LAYER_OPACITY = builder
                .comment("Normal cloud layer opacity. Defaults more transparent than the upper layer.")
                .defineInRange("lowerLayerOpacity", 0.60, 0.05, 1.0);
        UPPER_LAYER_ENABLED = builder
                .comment("Render a second, independently sampled cloud layer above the dimension's normal clouds.")
                .define("upperLayerEnabled", true);
        UPPER_LAYER_QUALITY = builder
                .comment("Geometry quality for the upper cloud layer.")
                .defineEnum("upperLayerQuality", CloudQuality.FANCY);
        UPPER_LAYER_HEIGHT_OFFSET = builder
                .comment("Upper cloud layer height above the normal cloud layer, in blocks.")
                .defineInRange("upperLayerHeightOffset", 64.0, 16.0, 256.0);
        UPPER_LAYER_SPEED = builder
                .comment("Upper cloud layer speed multiplier. Defaults slower than the normal layer.")
                .defineInRange("upperLayerSpeed", 0.55, 0.0, 4.0);
        UPPER_LAYER_OPACITY = builder
                .comment("Upper cloud layer opacity.")
                .defineInRange("upperLayerOpacity", 0.85, 0.05, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private CirrusConfig() {
    }
}
