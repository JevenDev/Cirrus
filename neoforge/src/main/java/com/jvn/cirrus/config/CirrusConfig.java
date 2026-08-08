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
    public static final DoubleSetting TIME_TRANSITION_SPEED_SETTING = new DoubleSetting(1.0, 0.25, 4.0, 0.05);
    public static final IntSetting STAR_DENSITY_SETTING = new IntSetting(2000, 250, 8000, 250);
    public static final DoubleSetting STAR_MIN_OPACITY_SETTING = new DoubleSetting(0.25, 0.0, 1.0, 0.05);
    public static final DoubleSetting STAR_MAX_OPACITY_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);
    public static final DoubleSetting STAR_MIN_SIZE_SETTING = new DoubleSetting(0.55, 0.25, 4.0, 0.05);
    public static final DoubleSetting STAR_MAX_SIZE_SETTING = new DoubleSetting(2.0, 0.25, 4.0, 0.05);
    public static final DoubleSetting STAR_TWINKLE_STRENGTH_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);
    public static final DoubleSetting STAR_TWINKLE_SPEED_SETTING = new DoubleSetting(1.0, 0.0, 3.0, 0.05);
    public static final DoubleSetting STAR_COLOR_VARIATION_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);
    public static final DoubleSetting SHOOTING_STAR_FREQUENCY_SETTING = new DoubleSetting(1.0, 0.0, 12.0, 0.25);
    public static final DoubleSetting SHOOTING_STAR_OPACITY_SETTING = new DoubleSetting(0.85, 0.0, 1.0, 0.05);
    public static final DoubleSetting SHOOTING_STAR_MIN_SIZE_SETTING = new DoubleSetting(0.75, 0.25, 4.0, 0.05);
    public static final DoubleSetting SHOOTING_STAR_MAX_SIZE_SETTING = new DoubleSetting(1.5, 0.25, 4.0, 0.05);
    public static final DoubleSetting SHOOTING_STAR_MIN_SPEED_SETTING = new DoubleSetting(0.65, 0.25, 3.0, 0.05);
    public static final DoubleSetting SHOOTING_STAR_MAX_SPEED_SETTING = new DoubleSetting(1.35, 0.25, 3.0, 0.05);
    public static final DoubleSetting SHOOTING_STAR_TRAIL_LENGTH_SETTING = new DoubleSetting(3.0, 0.25, 3.0, 0.05);
    public static final DoubleSetting SHOOTING_STAR_BLOOM_SETTING = new DoubleSetting(2.0, 0.0, 2.0, 0.05);
    public static final DoubleSetting SHOOTING_STAR_COLOR_VARIATION_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);
    public static final DoubleSetting NIGHT_SKY_COLOR_OPACITY_SETTING = new DoubleSetting(0.55, 0.0, 1.0, 0.05);
    public static final DoubleSetting MILKY_WAY_OPACITY_SETTING = new DoubleSetting(0.65, 0.0, 1.0, 0.05);
    public static final DoubleSetting AURORA_OPACITY_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);
    public static final DoubleSetting AURORA_ANIMATION_SPEED_SETTING = new DoubleSetting(5.0, 0.0, 5.0, 0.05);
    public static final DoubleSetting AURORA_MOVEMENT_SETTING = new DoubleSetting(2.0, 0.0, 2.0, 0.05);
    public static final DoubleSetting AURORA_RIBBON_WIDTH_SETTING = new DoubleSetting(1.0, 0.5, 2.0, 0.05);
    public static final DoubleSetting AURORA_HEIGHT_SETTING = new DoubleSetting(-10.0, -20.0, 30.0, 1.0);
    public static final DoubleSetting AURORA_NIGHTLY_VARIATION_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);
    public static final DoubleSetting END_SKY_INTENSITY_SETTING = new DoubleSetting(1.0, 0.0, 2.0, 0.05);
    public static final DoubleSetting END_SKY_ANIMATION_SPEED_SETTING = new DoubleSetting(1.0, 0.0, 3.0, 0.05);
    public static final DoubleSetting END_SKY_MORPH_SPEED_SETTING = new DoubleSetting(3.0, 0.0, 3.0, 0.05);
    public static final DoubleSetting END_SKY_VOID_COVERAGE_SETTING = new DoubleSetting(2.0, 0.0, 2.0, 0.05);
    public static final DoubleSetting END_SKY_VOID_DARKNESS_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);
    public static final DoubleSetting END_SKY_LIGHTNING_FREQUENCY_SETTING = new DoubleSetting(3.0, 0.0, 3.0, 0.05);
    public static final DoubleSetting END_SKY_LIGHTNING_INTENSITY_SETTING = new DoubleSetting(2.0, 0.0, 2.0, 0.05);
    public static final DoubleSetting END_SKY_SURGE_FREQUENCY_SETTING = new DoubleSetting(3.0, 0.0, 3.0, 0.05);
    public static final DoubleSetting END_SKY_SURGE_STRENGTH_SETTING = new DoubleSetting(1.0, 0.0, 1.0, 0.05);

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
    public static final ModConfigSpec.BooleanValue SMOOTH_TIME_TRANSITIONS;
    public static final ModConfigSpec.DoubleValue TIME_TRANSITION_SPEED;
    public static final ModConfigSpec.BooleanValue CUSTOM_STARS_ENABLED;
    public static final ModConfigSpec.IntValue STAR_DENSITY;
    public static final ModConfigSpec.DoubleValue STAR_MIN_OPACITY;
    public static final ModConfigSpec.DoubleValue STAR_MAX_OPACITY;
    public static final ModConfigSpec.DoubleValue STAR_MIN_SIZE;
    public static final ModConfigSpec.DoubleValue STAR_MAX_SIZE;
    public static final ModConfigSpec.DoubleValue STAR_TWINKLE_STRENGTH;
    public static final ModConfigSpec.DoubleValue STAR_TWINKLE_SPEED;
    public static final ModConfigSpec.DoubleValue STAR_COLOR_VARIATION;
    public static final ModConfigSpec.BooleanValue SHOOTING_STARS_ENABLED;
    public static final ModConfigSpec.DoubleValue SHOOTING_STAR_FREQUENCY;
    public static final ModConfigSpec.DoubleValue SHOOTING_STAR_OPACITY;
    public static final ModConfigSpec.DoubleValue SHOOTING_STAR_MIN_SIZE;
    public static final ModConfigSpec.DoubleValue SHOOTING_STAR_MAX_SIZE;
    public static final ModConfigSpec.DoubleValue SHOOTING_STAR_MIN_SPEED;
    public static final ModConfigSpec.DoubleValue SHOOTING_STAR_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue SHOOTING_STAR_TRAIL_LENGTH;
    public static final ModConfigSpec.DoubleValue SHOOTING_STAR_BLOOM;
    public static final ModConfigSpec.DoubleValue SHOOTING_STAR_COLOR_VARIATION;
    public static final ModConfigSpec.BooleanValue NIGHT_SKY_COLORS_ENABLED;
    public static final ModConfigSpec.DoubleValue NIGHT_SKY_COLOR_OPACITY;
    public static final ModConfigSpec.BooleanValue MILKY_WAY_ENABLED;
    public static final ModConfigSpec.BooleanValue MILKY_WAY_PIXELATION_ENABLED;
    public static final ModConfigSpec.DoubleValue MILKY_WAY_OPACITY;
    public static final ModConfigSpec.BooleanValue AURORA_ENABLED;
    public static final ModConfigSpec.BooleanValue AURORA_PIXELATION_ENABLED;
    public static final ModConfigSpec.BooleanValue AURORA_COLD_BIOMES_ONLY;
    public static final ModConfigSpec.DoubleValue AURORA_OPACITY;
    public static final ModConfigSpec.DoubleValue AURORA_ANIMATION_SPEED;
    public static final ModConfigSpec.DoubleValue AURORA_MOVEMENT;
    public static final ModConfigSpec.DoubleValue AURORA_RIBBON_WIDTH;
    public static final ModConfigSpec.DoubleValue AURORA_HEIGHT_DEGREES;
    public static final ModConfigSpec.DoubleValue AURORA_NIGHTLY_VARIATION;
    public static final ModConfigSpec.BooleanValue END_SKY_ENABLED;
    public static final ModConfigSpec.BooleanValue END_SKY_PIXELATION_ENABLED;
    public static final ModConfigSpec.DoubleValue END_SKY_INTENSITY;
    public static final ModConfigSpec.DoubleValue END_SKY_ANIMATION_SPEED;
    public static final ModConfigSpec.DoubleValue END_SKY_MORPH_SPEED;
    public static final ModConfigSpec.DoubleValue END_SKY_VOID_COVERAGE;
    public static final ModConfigSpec.DoubleValue END_SKY_VOID_DARKNESS;
    public static final ModConfigSpec.DoubleValue END_SKY_LIGHTNING_FREQUENCY;
    public static final ModConfigSpec.DoubleValue END_SKY_LIGHTNING_INTENSITY;
    public static final ModConfigSpec.DoubleValue END_SKY_SURGE_FREQUENCY;
    public static final ModConfigSpec.DoubleValue END_SKY_SURGE_STRENGTH;
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
        SMOOTH_TIME_TRANSITIONS = builder
                .comment("Smooth abrupt visual changes in the sky when the world's day time changes.")
                .define("smoothTimeTransitions", true);
        TIME_TRANSITION_SPEED = TIME_TRANSITION_SPEED_SETTING.define(
                builder,
                "timeTransitionSpeed",
                "Speed multiplier for smooth time transitions. Higher values complete transitions faster."
        );
        CUSTOM_STARS_ENABLED = builder
                .comment("Replace vanilla stars with Cirrus' shader-driven star field.")
                .define("customStarsEnabled", true);
        STAR_DENSITY = STAR_DENSITY_SETTING.define(
                builder,
                "starDensity",
                "Maximum number of stars distributed across the complete celestial sphere."
        );
        STAR_MIN_OPACITY = STAR_MIN_OPACITY_SETTING.define(
                builder,
                "starMinimumOpacity",
                "Opacity of the dimmest stars."
        );
        STAR_MAX_OPACITY = STAR_MAX_OPACITY_SETTING.define(
                builder,
                "starMaximumOpacity",
                "Opacity of the brightest stars."
        );
        STAR_MIN_SIZE = STAR_MIN_SIZE_SETTING.define(
                builder,
                "starMinimumSize",
                "Size multiplier used by the smallest stars."
        );
        STAR_MAX_SIZE = STAR_MAX_SIZE_SETTING.define(
                builder,
                "starMaximumSize",
                "Size multiplier used by the largest stars."
        );
        STAR_TWINKLE_STRENGTH = STAR_TWINKLE_STRENGTH_SETTING.define(
                builder,
                "starTwinkleStrength",
                "Amount that individual stars gently vary in brightness."
        );
        STAR_TWINKLE_SPEED = STAR_TWINKLE_SPEED_SETTING.define(
                builder,
                "starTwinkleSpeed",
                "Speed of the star twinkle animation. Zero freezes the animation."
        );
        STAR_COLOR_VARIATION = STAR_COLOR_VARIATION_SETTING.define(
                builder,
                "starColorVariation",
                "Strength of subtle warm and cool color differences between stars."
        );
        SHOOTING_STARS_ENABLED = builder
                .comment("Occasionally render shader-animated shooting stars at night.")
                .define("shootingStarsEnabled", true);
        SHOOTING_STAR_FREQUENCY = SHOOTING_STAR_FREQUENCY_SETTING.define(
                builder,
                "shootingStarFrequency",
                "Average shooting-star events per real-world minute. Zero disables events."
        );
        SHOOTING_STAR_OPACITY = SHOOTING_STAR_OPACITY_SETTING.define(
                builder,
                "shootingStarOpacity",
                "Maximum opacity of shooting-star heads and trails."
        );
        SHOOTING_STAR_MIN_SIZE = SHOOTING_STAR_MIN_SIZE_SETTING.define(
                builder,
                "shootingStarMinimumSize",
                "Size multiplier used by the smallest shooting stars."
        );
        SHOOTING_STAR_MAX_SIZE = SHOOTING_STAR_MAX_SIZE_SETTING.define(
                builder,
                "shootingStarMaximumSize",
                "Size multiplier used by the largest shooting stars."
        );
        SHOOTING_STAR_MIN_SPEED = SHOOTING_STAR_MIN_SPEED_SETTING.define(
                builder,
                "shootingStarMinimumSpeed",
                "Speed multiplier used by the slowest shooting stars."
        );
        SHOOTING_STAR_MAX_SPEED = SHOOTING_STAR_MAX_SPEED_SETTING.define(
                builder,
                "shootingStarMaximumSpeed",
                "Speed multiplier used by the fastest shooting stars."
        );
        SHOOTING_STAR_TRAIL_LENGTH = SHOOTING_STAR_TRAIL_LENGTH_SETTING.define(
                builder,
                "shootingStarTrailLength",
                "Length multiplier for shooting-star trails."
        );
        SHOOTING_STAR_BLOOM = SHOOTING_STAR_BLOOM_SETTING.define(
                builder,
                "shootingStarBloom",
                "Strength of the soft glow around shooting-star cores and trails."
        );
        SHOOTING_STAR_COLOR_VARIATION = SHOOTING_STAR_COLOR_VARIATION_SETTING.define(
                builder,
                "shootingStarColorVariation",
                "Strength of warm and cool color variation between shooting stars."
        );
        NIGHT_SKY_COLORS_ENABLED = builder
                .comment("Tint the night sky with a restrained indigo, mauve, and gold palette.")
                .define("nightSkyColorsEnabled", true);
        NIGHT_SKY_COLOR_OPACITY = NIGHT_SKY_COLOR_OPACITY_SETTING.define(
                builder,
                "nightSkyColorOpacity",
                "Strength of the custom night-sky color palette."
        );
        MILKY_WAY_ENABLED = builder
                .comment("Render a shader-driven Milky Way behind clouds and northern lights.")
                .define("milkyWayEnabled", true);
        MILKY_WAY_PIXELATION_ENABLED = builder
                .comment("Render the Milky Way through a sky-fixed pixel grid.")
                .define("milkyWayPixelationEnabled", true);
        MILKY_WAY_OPACITY = MILKY_WAY_OPACITY_SETTING.define(
                builder,
                "milkyWayOpacity",
                "Maximum opacity of the Milky Way's galactic haze."
        );
        AURORA_ENABLED = builder
                .comment("Render animated northern lights at night. Biome restrictions are configured separately.")
                .define("auroraEnabled", true);
        AURORA_PIXELATION_ENABLED = builder
                .comment("Render northern lights through a sky-fixed pixel grid.")
                .define("auroraPixelationEnabled", true);
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
        END_SKY_ENABLED = builder
                .comment("Replace the vanilla End sky with moving clouds, lightning, and spreading darkness.")
                .define("endSkyEnabled", true);
        END_SKY_PIXELATION_ENABLED = builder
                .comment("Sample the End sky through a sky-fixed pixel grid for a Minecraft-native finish.")
                .define("endSkyPixelationEnabled", true);
        END_SKY_INTENSITY = END_SKY_INTENSITY_SETTING.define(
                builder,
                "endSkyIntensity",
                "Brightness and color strength of the End sky's nebulas and storms."
        );
        END_SKY_ANIMATION_SPEED = END_SKY_ANIMATION_SPEED_SETTING.define(
                builder,
                "endSkyAnimationSpeed",
                "Speed multiplier for the End sky's layered drift. Zero freezes positional movement."
        );
        END_SKY_MORPH_SPEED = END_SKY_MORPH_SPEED_SETTING.define(
                builder, "endSkyMorphSpeed",
                "Speed multiplier for clouds reshaping and merging. Zero freezes shape changes."
        );
        END_SKY_VOID_COVERAGE = END_SKY_VOID_COVERAGE_SETTING.define(
                builder, "endSkyVoidCoverage",
                "Amount of the End sky occupied by morphing black voids."
        );
        END_SKY_VOID_DARKNESS = END_SKY_VOID_DARKNESS_SETTING.define(
                builder, "endSkyVoidDarkness",
                "Strength of the permanent black voids. Zero removes their darkening."
        );
        END_SKY_LIGHTNING_FREQUENCY = END_SKY_LIGHTNING_FREQUENCY_SETTING.define(
                builder, "endSkyLightningFrequency",
                "Frequency multiplier for chain and distant lightning. Zero disables both."
        );
        END_SKY_LIGHTNING_INTENSITY = END_SKY_LIGHTNING_INTENSITY_SETTING.define(
                builder, "endSkyLightningIntensity",
                "Brightness of End lightning bolts and their illumination."
        );
        END_SKY_SURGE_FREQUENCY = END_SKY_SURGE_FREQUENCY_SETTING.define(
                builder, "endSkySurgeFrequency",
                "Frequency multiplier for irregular darkness-spreading events. Zero disables them."
        );
        END_SKY_SURGE_STRENGTH = END_SKY_SURGE_STRENGTH_SETTING.define(
                builder, "endSkySurgeStrength",
                "Maximum strength of temporary darkness-spreading events."
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
