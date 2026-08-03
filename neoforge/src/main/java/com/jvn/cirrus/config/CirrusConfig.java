package com.jvn.cirrus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CirrusConfig {
    public static final IntSetting CLOUD_RENDER_DISTANCE_SETTING = new IntSetting(64, 2, 128, 1);
    public static final DoubleSetting LIGHTNING_CLOUD_FLASH_OPACITY_SETTING =
            new DoubleSetting(0.75, 0.0, 1.0, 0.05);
    public static final DoubleSetting LOWER_LAYER_HEIGHT_SETTING = new DoubleSetting(0.0, -128.0, 128.0, 1.0);
    public static final DoubleSetting LOWER_LAYER_SPEED_SETTING = new DoubleSetting(2.0, 0.0, 4.0, 0.05);
    public static final DoubleSetting LOWER_LAYER_OPACITY_SETTING = new DoubleSetting(0.25, 0.05, 1.0, 0.05);
    public static final DoubleSetting UPPER_LAYER_HEIGHT_SETTING = new DoubleSetting(64.0, 16.0, 256.0, 1.0);
    public static final DoubleSetting UPPER_LAYER_SPEED_SETTING = new DoubleSetting(1.0, 0.0, 4.0, 0.05);
    public static final DoubleSetting UPPER_LAYER_OPACITY_SETTING = new DoubleSetting(0.50, 0.05, 1.0, 0.05);
    public static final DoubleSetting TOP_LAYER_HEIGHT_SETTING = new DoubleSetting(64.0, 16.0, 256.0, 1.0);
    public static final DoubleSetting TOP_LAYER_SPEED_SETTING = new DoubleSetting(0.50, 0.0, 4.0, 0.05);
    public static final DoubleSetting TOP_LAYER_OPACITY_SETTING = new DoubleSetting(0.15, 0.05, 1.0, 0.05);
    public static final DoubleSetting AURORA_OPACITY_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);
    public static final DoubleSetting AURORA_ANIMATION_SPEED_SETTING = new DoubleSetting(5.0, 0.0, 5.0, 0.05);
    public static final DoubleSetting AURORA_MOVEMENT_SETTING = new DoubleSetting(2.0, 0.0, 2.0, 0.05);
    public static final DoubleSetting AURORA_RIBBON_WIDTH_SETTING = new DoubleSetting(1.0, 0.5, 2.0, 0.05);
    public static final DoubleSetting AURORA_HEIGHT_SETTING = new DoubleSetting(-10.0, -20.0, 30.0, 1.0);
    public static final DoubleSetting AURORA_NIGHTLY_VARIATION_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);

    public static final ModConfigSpec.BooleanValue CUSTOM_CLOUDS_ENABLED;
    public static final ModConfigSpec.IntValue CLOUD_RENDER_DISTANCE;
    public static final ModConfigSpec.BooleanValue HIDE_LIGHTNING_CLOUD_FLASHES;
    public static final ModConfigSpec.DoubleValue LIGHTNING_CLOUD_FLASH_OPACITY;
    public static final ModConfigSpec.DoubleValue LOWER_LAYER_HEIGHT_OFFSET;
    public static final ModConfigSpec.DoubleValue LOWER_LAYER_SPEED;
    public static final ModConfigSpec.DoubleValue LOWER_LAYER_OPACITY;
    public static final ModConfigSpec.BooleanValue UPPER_LAYER_ENABLED;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_HEIGHT_OFFSET;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_SPEED;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_OPACITY;
    public static final ModConfigSpec.BooleanValue TOP_LAYER_ENABLED;
    public static final ModConfigSpec.DoubleValue TOP_LAYER_HEIGHT_OFFSET;
    public static final ModConfigSpec.DoubleValue TOP_LAYER_SPEED;
    public static final ModConfigSpec.DoubleValue TOP_LAYER_OPACITY;
    public static final ModConfigSpec.BooleanValue AURORA_ENABLED;
    public static final ModConfigSpec.BooleanValue AURORA_COLD_BIOMES_ONLY;
    public static final ModConfigSpec.DoubleValue AURORA_OPACITY;
    public static final ModConfigSpec.DoubleValue AURORA_ANIMATION_SPEED;
    public static final ModConfigSpec.DoubleValue AURORA_MOVEMENT;
    public static final ModConfigSpec.DoubleValue AURORA_RIBBON_WIDTH;
    public static final ModConfigSpec.DoubleValue AURORA_HEIGHT_DEGREES;
    public static final ModConfigSpec.DoubleValue AURORA_NIGHTLY_VARIATION;
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
        HIDE_LIGHTNING_CLOUD_FLASHES = builder
                .comment("Hide Cirrus cloud illumination during lightning without changing the vanilla sky flash.")
                .define("hideLightningCloudFlashes", false);
        LIGHTNING_CLOUD_FLASH_OPACITY = LIGHTNING_CLOUD_FLASH_OPACITY_SETTING.define(
                builder,
                "lightningCloudFlashOpacity",
                "Maximum opacity of lightning illumination on nearby clouds."
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
        TOP_LAYER_ENABLED = builder
                .comment("Render a third, independently sampled cloud layer above the upper cloud layer.")
                .define("topLayerEnabled", true);
        TOP_LAYER_HEIGHT_OFFSET = TOP_LAYER_HEIGHT_SETTING.define(
                builder,
                "topLayerHeightOffset",
                "Top cloud layer height above the upper cloud layer, in blocks."
        );
        TOP_LAYER_SPEED = TOP_LAYER_SPEED_SETTING.define(
                builder,
                "topLayerSpeed",
                "Top cloud layer speed multiplier. Defaults slower than the upper layer."
        );
        TOP_LAYER_OPACITY = TOP_LAYER_OPACITY_SETTING.define(
                builder,
                "topLayerOpacity",
                "Top cloud layer opacity. Defaults more transparent than the other layers."
        );
        builder.pop();

        builder.comment("Sky rendering").push("sky");
        AURORA_ENABLED = builder
                .comment("Render animated northern lights at night. Biome restrictions are configured separately.")
                .define("auroraEnabled", true);
        AURORA_COLD_BIOMES_ONLY = builder
                .comment("Only render northern lights while the camera is in or near a freezing biome.")
                .define("auroraColdBiomesOnly", true);
        AURORA_OPACITY = AURORA_OPACITY_SETTING.define(
                builder,
                "auroraOpacity",
                "Maximum opacity of the northern lights."
        );
        AURORA_ANIMATION_SPEED = AURORA_ANIMATION_SPEED_SETTING.define(
                builder,
                "auroraAnimationSpeed",
                "Animation speed multiplier. Zero freezes the aurora in place."
        );
        AURORA_MOVEMENT = AURORA_MOVEMENT_SETTING.define(
                builder,
                "auroraMovement",
                "Amount of drifting, waving, and breathing in the aurora."
        );
        AURORA_RIBBON_WIDTH = AURORA_RIBBON_WIDTH_SETTING.define(
                builder,
                "auroraRibbonWidth",
                "Width multiplier for the aurora ribbons."
        );
        AURORA_HEIGHT_DEGREES = AURORA_HEIGHT_SETTING.define(
                builder,
                "auroraHeightDegrees",
                "Vertical offset of the aurora in degrees."
        );
        AURORA_NIGHTLY_VARIATION = AURORA_NIGHTLY_VARIATION_SETTING.define(
                builder,
                "auroraNightlyVariation",
                "How strongly the aurora layout changes from one night to the next."
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
