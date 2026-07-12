package com.jvn.cirrus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CirrusConfig {
    public static final IntSetting CLOUD_RENDER_DISTANCE_SETTING = new IntSetting(64, 2, 128, 1);
    public static final DoubleSetting LOWER_LAYER_HEIGHT_SETTING = new DoubleSetting(0.0, -128.0, 128.0, 1.0);
    public static final DoubleSetting LOWER_LAYER_SPEED_SETTING = new DoubleSetting(1.0, 0.0, 4.0, 0.05);
    public static final DoubleSetting LOWER_LAYER_OPACITY_SETTING = new DoubleSetting(0.25, 0.05, 1.0, 0.05);
    public static final DoubleSetting UPPER_LAYER_HEIGHT_SETTING = new DoubleSetting(64.0, 16.0, 256.0, 1.0);
    public static final DoubleSetting UPPER_LAYER_SPEED_SETTING = new DoubleSetting(0.55, 0.0, 4.0, 0.05);
    public static final DoubleSetting UPPER_LAYER_OPACITY_SETTING = new DoubleSetting(0.50, 0.05, 1.0, 0.05);

    public static final ModConfigSpec.BooleanValue CUSTOM_CLOUDS_ENABLED;
    public static final ModConfigSpec.IntValue CLOUD_RENDER_DISTANCE;
    public static final ModConfigSpec.DoubleValue LOWER_LAYER_HEIGHT_OFFSET;
    public static final ModConfigSpec.DoubleValue LOWER_LAYER_SPEED;
    public static final ModConfigSpec.DoubleValue LOWER_LAYER_OPACITY;
    public static final ModConfigSpec.BooleanValue UPPER_LAYER_ENABLED;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_HEIGHT_OFFSET;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_SPEED;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_OPACITY;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Cloud rendering").push("clouds");
        CUSTOM_CLOUDS_ENABLED = builder
                .comment("Replace vanilla clouds with Cirrus clouds. The vanilla Clouds: Off setting still hides clouds.")
                .define("enabled", true);
        CLOUD_RENDER_DISTANCE = CLOUD_RENDER_DISTANCE_SETTING.define(
                builder,
                "renderDistanceChunks",
                "Cloud render distance in chunks, independent of terrain render distance."
        );
        LOWER_LAYER_HEIGHT_OFFSET = LOWER_LAYER_HEIGHT_SETTING.define(
                builder,
                "lowerLayerHeightOffset",
                "Normal cloud layer height offset from the dimension's cloud height, in blocks."
        );
        LOWER_LAYER_SPEED = LOWER_LAYER_SPEED_SETTING.define(
                builder,
                "lowerLayerSpeed",
                "Normal cloud layer speed multiplier. Zero makes the layer stationary."
        );
        LOWER_LAYER_OPACITY = LOWER_LAYER_OPACITY_SETTING.define(
                builder,
                "lowerLayerOpacity",
                "Normal cloud layer opacity. Defaults more transparent than the upper layer."
        );
        UPPER_LAYER_ENABLED = builder
                .comment("Render a second, independently sampled cloud layer above the dimension's normal clouds.")
                .define("upperLayerEnabled", true);
        UPPER_LAYER_HEIGHT_OFFSET = UPPER_LAYER_HEIGHT_SETTING.define(
                builder,
                "upperLayerHeightOffset",
                "Upper cloud layer height above the normal cloud layer, in blocks."
        );
        UPPER_LAYER_SPEED = UPPER_LAYER_SPEED_SETTING.define(
                builder,
                "upperLayerSpeed",
                "Upper cloud layer speed multiplier. Defaults slower than the normal layer."
        );
        UPPER_LAYER_OPACITY = UPPER_LAYER_OPACITY_SETTING.define(
                builder,
                "upperLayerOpacity",
                "Upper cloud layer opacity."
        );
        builder.pop();
        SPEC = builder.build();
    }

    private CirrusConfig() {
    }

    public record IntSetting(int defaultValue, int minimum, int maximum, int step) {
        private ModConfigSpec.IntValue define(ModConfigSpec.Builder builder, String name, String comment) {
            return builder.comment(comment).defineInRange(name, defaultValue, minimum, maximum);
        }
    }

    public record DoubleSetting(double defaultValue, double minimum, double maximum, double step) {
        private ModConfigSpec.DoubleValue define(ModConfigSpec.Builder builder, String name, String comment) {
            return builder.comment(comment).defineInRange(name, defaultValue, minimum, maximum);
        }
    }
}
