package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import java.awt.Color;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class CirrusConfigScreen {
    private CirrusConfigScreen() {
    }

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(text("cirrus.config.title"))
                .category(cloudCategory())
                .category(skyColorCategory())
                .category(skyCategory())
                .save(CirrusConfig.SPEC::save)
                .build()
                .generateScreen(parent);
    }

    private static ConfigCategory cloudCategory() {
        return ConfigCategory.createBuilder()
                .name(text("cirrus.config.category.clouds"))
                .tooltip(text("cirrus.config.category.clouds.description"))
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.cloudGeneral"))
                        .option(booleanOption("cirrus.config.clouds.enabled", CirrusConfig.CUSTOM_CLOUDS_ENABLED))
                        .option(booleanOption(
                                "cirrus.config.clouds.distantHorizonsCompatibility",
                                CirrusConfig.DISTANT_HORIZONS_COMPATIBILITY
                        ))
                        .option(booleanOption(
                                "cirrus.config.clouds.syncCloudDistanceWithDistantHorizons",
                                CirrusConfig.SYNC_CLOUD_DISTANCE_WITH_DISTANT_HORIZONS,
                                warningDescription("cirrus.config.clouds.syncCloudDistanceWithDistantHorizons")
                        ))
                        .option(integerOption(
                                "cirrus.config.clouds.renderDistanceChunks",
                                CirrusConfig.CLOUD_RENDER_DISTANCE,
                                CirrusConfig.CLOUD_RENDER_DISTANCE_SETTING,
                                value -> Component.literal(value + " chunks")
                        ))
                        .option(booleanOption(
                                "cirrus.config.clouds.translucentLayerOverlap",
                                CirrusConfig.TRANSLUCENT_LAYER_OVERLAP
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.cloudWeather"))
                        .option(percentageOption(
                                "cirrus.config.clouds.rainCloudCoverage",
                                CirrusConfig.RAIN_CLOUD_COVERAGE,
                                CirrusConfig.RAIN_CLOUD_COVERAGE_SETTING
                        ))
                        .option(percentageOption(
                                "cirrus.config.clouds.thunderCloudCoverage",
                                CirrusConfig.THUNDER_CLOUD_COVERAGE,
                                CirrusConfig.THUNDER_CLOUD_COVERAGE_SETTING
                        ))
                        .option(booleanOption(
                                "cirrus.config.clouds.customLightningEnabled",
                                CirrusConfig.CUSTOM_LIGHTNING_ENABLED
                        ))
                        .option(doubleOption(
                                "cirrus.config.clouds.lightningBoltIntensity",
                                CirrusConfig.LIGHTNING_BOLT_INTENSITY,
                                CirrusConfig.LIGHTNING_BOLT_INTENSITY_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(booleanOption(
                                "cirrus.config.clouds.hideLightningCloudFlashes",
                                CirrusConfig.HIDE_LIGHTNING_CLOUD_FLASHES
                        ))
                        .option(percentageOption(
                                "cirrus.config.clouds.lightningCloudFlashOpacity",
                                CirrusConfig.LIGHTNING_CLOUD_FLASH_OPACITY,
                                CirrusConfig.LIGHTNING_CLOUD_FLASH_OPACITY_SETTING
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.lowerLayer"))
                        .option(enumOption(
                                "cirrus.config.clouds.lowerLayerStyle",
                                CirrusConfig.LOWER_LAYER_STYLE,
                                CirrusConfig.CloudStyle.class,
                                style -> Component.translatable("options.clouds." + style.name().toLowerCase(Locale.ROOT))
                        ))
                        .option(doubleOption(
                                "cirrus.config.clouds.lowerLayerHeightOffset",
                                CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET,
                                CirrusConfig.LOWER_LAYER_HEIGHT_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.0f blocks", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.clouds.lowerLayerSpeed",
                                CirrusConfig.LOWER_LAYER_SPEED,
                                CirrusConfig.LOWER_LAYER_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.clouds.lowerLayerOpacity",
                                CirrusConfig.LOWER_LAYER_OPACITY,
                                CirrusConfig.LOWER_LAYER_OPACITY_SETTING
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.upperLayer"))
                        .option(booleanOption("cirrus.config.clouds.upperLayerEnabled", CirrusConfig.UPPER_LAYER_ENABLED))
                        .option(enumOption(
                                "cirrus.config.clouds.upperLayerStyle",
                                CirrusConfig.UPPER_LAYER_STYLE,
                                CirrusConfig.CloudStyle.class,
                                style -> Component.translatable("options.clouds." + style.name().toLowerCase(Locale.ROOT))
                        ))
                        .option(doubleOption(
                                "cirrus.config.clouds.upperLayerHeightOffset",
                                CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET,
                                CirrusConfig.UPPER_LAYER_HEIGHT_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.0f blocks", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.clouds.upperLayerSpeed",
                                CirrusConfig.UPPER_LAYER_SPEED,
                                CirrusConfig.UPPER_LAYER_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.clouds.upperLayerOpacity",
                                CirrusConfig.UPPER_LAYER_OPACITY,
                                CirrusConfig.UPPER_LAYER_OPACITY_SETTING
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.topLayer"))
                        .option(booleanOption("cirrus.config.clouds.topLayerEnabled", CirrusConfig.TOP_LAYER_ENABLED))
                        .option(enumOption(
                                "cirrus.config.clouds.topLayerStyle",
                                CirrusConfig.TOP_LAYER_STYLE,
                                CirrusConfig.CloudStyle.class,
                                style -> Component.translatable("options.clouds." + style.name().toLowerCase(Locale.ROOT))
                        ))
                        .option(doubleOption(
                                "cirrus.config.clouds.topLayerHeightOffset",
                                CirrusConfig.TOP_LAYER_HEIGHT_OFFSET,
                                CirrusConfig.TOP_LAYER_HEIGHT_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.0f blocks", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.clouds.topLayerSpeed",
                                CirrusConfig.TOP_LAYER_SPEED,
                                CirrusConfig.TOP_LAYER_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.clouds.topLayerOpacity",
                                CirrusConfig.TOP_LAYER_OPACITY,
                                CirrusConfig.TOP_LAYER_OPACITY_SETTING
                        ))
                        .build())
                .build();
    }

    private static ConfigCategory skyColorCategory() {
        return ConfigCategory.createBuilder()
                .name(text("cirrus.config.category.skyColors"))
                .tooltip(text("cirrus.config.category.skyColors.description"))
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.skyGradientGeneral"))
                        .option(booleanOption(
                                "cirrus.config.sky.skyGradientsEnabled",
                                CirrusConfig.SKY_GRADIENTS_ENABLED
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.skyGradientOpacity",
                                CirrusConfig.SKY_GRADIENT_OPACITY,
                                CirrusConfig.SKY_GRADIENT_OPACITY_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.skyGradientTransitionTicks",
                                CirrusConfig.SKY_GRADIENT_TRANSITION_TICKS,
                                CirrusConfig.SKY_GRADIENT_TRANSITION_TICKS_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.0f ticks", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.skyGradientHeight",
                                CirrusConfig.SKY_GRADIENT_HEIGHT,
                                CirrusConfig.SKY_GRADIENT_HEIGHT_SETTING
                        ))
                        .build())
                .group(gradientPhaseGroup(
                        "morning",
                        CirrusConfig.MORNING_HORIZON_COLOR,
                        CirrusConfig.MORNING_ZENITH_COLOR,
                        CirrusConfig.MORNING_GRADIENT_STRENGTH,
                        CirrusConfig.MORNING_GRADIENT_STRENGTH_SETTING
                ))
                .group(gradientPhaseGroup(
                        "day",
                        CirrusConfig.DAY_HORIZON_COLOR,
                        CirrusConfig.DAY_ZENITH_COLOR,
                        CirrusConfig.DAY_GRADIENT_STRENGTH,
                        CirrusConfig.DAY_GRADIENT_STRENGTH_SETTING
                ))
                .group(gradientPhaseGroup(
                        "evening",
                        CirrusConfig.EVENING_HORIZON_COLOR,
                        CirrusConfig.EVENING_ZENITH_COLOR,
                        CirrusConfig.EVENING_GRADIENT_STRENGTH,
                        CirrusConfig.EVENING_GRADIENT_STRENGTH_SETTING
                ))
                .group(gradientPhaseGroup(
                        "night",
                        CirrusConfig.NIGHT_HORIZON_COLOR,
                        CirrusConfig.NIGHT_ZENITH_COLOR,
                        CirrusConfig.NIGHT_GRADIENT_STRENGTH,
                        CirrusConfig.NIGHT_GRADIENT_STRENGTH_SETTING
                ))
                .build();
    }

    private static OptionGroup gradientPhaseGroup(
            String phase,
            ModConfigSpec.IntValue horizonColor,
            ModConfigSpec.IntValue zenithColor,
            ModConfigSpec.DoubleValue strength,
            CirrusConfig.DoubleSetting strengthSetting
    ) {
        String key = "cirrus.config.sky." + phase;
        return OptionGroup.createBuilder()
                .name(text("cirrus.config.group." + phase + "Gradient"))
                .option(colorOption(key + "HorizonColor", horizonColor))
                .option(colorOption(key + "ZenithColor", zenithColor))
                .option(percentageOption(key + "GradientStrength", strength, strengthSetting))
                .build();
    }

    private static ConfigCategory skyCategory() {
        return ConfigCategory.createBuilder()
                .name(text("cirrus.config.category.sky"))
                .tooltip(text("cirrus.config.category.sky.description"))
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.time"))
                        .option(booleanOption(
                                "cirrus.config.sky.smoothTimeTransitions",
                                CirrusConfig.SMOOTH_TIME_TRANSITIONS
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.timeTransitionSpeed",
                                CirrusConfig.TIME_TRANSITION_SPEED,
                                CirrusConfig.TIME_TRANSITION_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.stars"))
                        .option(booleanOption(
                                "cirrus.config.sky.customStarsEnabled",
                                CirrusConfig.CUSTOM_STARS_ENABLED
                        ))
                        .option(integerOption(
                                "cirrus.config.sky.starDensity",
                                CirrusConfig.STAR_DENSITY,
                                CirrusConfig.STAR_DENSITY_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%,d stars", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.starMinimumOpacity",
                                CirrusConfig.STAR_MIN_OPACITY,
                                CirrusConfig.STAR_MIN_OPACITY_SETTING
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.starMaximumOpacity",
                                CirrusConfig.STAR_MAX_OPACITY,
                                CirrusConfig.STAR_MAX_OPACITY_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.starMinimumSize",
                                CirrusConfig.STAR_MIN_SIZE,
                                CirrusConfig.STAR_MIN_SIZE_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.starMaximumSize",
                                CirrusConfig.STAR_MAX_SIZE,
                                CirrusConfig.STAR_MAX_SIZE_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.starTwinkleStrength",
                                CirrusConfig.STAR_TWINKLE_STRENGTH,
                                CirrusConfig.STAR_TWINKLE_STRENGTH_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.starTwinkleSpeed",
                                CirrusConfig.STAR_TWINKLE_SPEED,
                                CirrusConfig.STAR_TWINKLE_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.starColorVariation",
                                CirrusConfig.STAR_COLOR_VARIATION,
                                CirrusConfig.STAR_COLOR_VARIATION_SETTING
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.shootingStars"))
                        .option(booleanOption(
                                "cirrus.config.sky.shootingStarsEnabled",
                                CirrusConfig.SHOOTING_STARS_ENABLED
                        ))
                        .option(booleanOption(
                                "cirrus.config.sky.shootingStarPixelatedTrail",
                                CirrusConfig.SHOOTING_STAR_PIXELATED_TRAIL
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.shootingStarFrequency",
                                CirrusConfig.SHOOTING_STAR_FREQUENCY,
                                CirrusConfig.SHOOTING_STAR_FREQUENCY_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2f/min", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.shootingStarOpacity",
                                CirrusConfig.SHOOTING_STAR_OPACITY,
                                CirrusConfig.SHOOTING_STAR_OPACITY_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.shootingStarMinimumSize",
                                CirrusConfig.SHOOTING_STAR_MIN_SIZE,
                                CirrusConfig.SHOOTING_STAR_MIN_SIZE_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.shootingStarMaximumSize",
                                CirrusConfig.SHOOTING_STAR_MAX_SIZE,
                                CirrusConfig.SHOOTING_STAR_MAX_SIZE_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.shootingStarMinimumSpeed",
                                CirrusConfig.SHOOTING_STAR_MIN_SPEED,
                                CirrusConfig.SHOOTING_STAR_MIN_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.shootingStarMaximumSpeed",
                                CirrusConfig.SHOOTING_STAR_MAX_SPEED,
                                CirrusConfig.SHOOTING_STAR_MAX_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.shootingStarSpeedVariation",
                                CirrusConfig.SHOOTING_STAR_SPEED_VARIATION,
                                CirrusConfig.SHOOTING_STAR_SPEED_VARIATION_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.shootingStarTrailLength",
                                CirrusConfig.SHOOTING_STAR_TRAIL_LENGTH,
                                CirrusConfig.SHOOTING_STAR_TRAIL_LENGTH_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.shootingStarBloom",
                                CirrusConfig.SHOOTING_STAR_BLOOM,
                                CirrusConfig.SHOOTING_STAR_BLOOM_SETTING
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.shootingStarColorVariation",
                                CirrusConfig.SHOOTING_STAR_COLOR_VARIATION,
                                CirrusConfig.SHOOTING_STAR_COLOR_VARIATION_SETTING
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.milkyWay"))
                        .option(booleanOption(
                                "cirrus.config.sky.milkyWayEnabled",
                                CirrusConfig.MILKY_WAY_ENABLED
                        ))
                        .option(booleanOption(
                                "cirrus.config.sky.milkyWayPixelationEnabled",
                                CirrusConfig.MILKY_WAY_PIXELATION_ENABLED
                        ))
                        .option(integerOption(
                                "cirrus.config.sky.milkyWayPixelationResolution",
                                CirrusConfig.MILKY_WAY_PIXELATION_RESOLUTION,
                                CirrusConfig.MILKY_WAY_PIXELATION_RESOLUTION_SETTING,
                                value -> Component.literal(value + " cells")
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.milkyWayOpacity",
                                CirrusConfig.MILKY_WAY_OPACITY,
                                CirrusConfig.MILKY_WAY_OPACITY_SETTING
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.aurora"))
                        .option(booleanOption("cirrus.config.sky.auroraEnabled", CirrusConfig.AURORA_ENABLED))
                        .option(booleanOption(
                                "cirrus.config.sky.auroraPixelationEnabled",
                                CirrusConfig.AURORA_PIXELATION_ENABLED
                        ))
                        .option(integerOption(
                                "cirrus.config.sky.auroraPixelationResolution",
                                CirrusConfig.AURORA_PIXELATION_RESOLUTION,
                                CirrusConfig.AURORA_PIXELATION_RESOLUTION_SETTING,
                                value -> Component.literal(value + " cells")
                        ))
                        .option(booleanOption(
                                "cirrus.config.sky.auroraColdBiomesOnly",
                                CirrusConfig.AURORA_COLD_BIOMES_ONLY
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.auroraOpacity",
                                CirrusConfig.AURORA_OPACITY,
                                CirrusConfig.AURORA_OPACITY_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.auroraAnimationSpeed",
                                CirrusConfig.AURORA_ANIMATION_SPEED,
                                CirrusConfig.AURORA_ANIMATION_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.auroraMovement",
                                CirrusConfig.AURORA_MOVEMENT,
                                CirrusConfig.AURORA_MOVEMENT_SETTING
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.auroraRibbonWidth",
                                CirrusConfig.AURORA_RIBBON_WIDTH,
                                CirrusConfig.AURORA_RIBBON_WIDTH_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.auroraHeight",
                                CirrusConfig.AURORA_HEIGHT_DEGREES,
                                CirrusConfig.AURORA_HEIGHT_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%+.0f degrees", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.auroraNightlyVariation",
                                CirrusConfig.AURORA_NIGHTLY_VARIATION,
                                CirrusConfig.AURORA_NIGHTLY_VARIATION_SETTING
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.endSky"))
                        .option(booleanOption("cirrus.config.sky.endSkyEnabled", CirrusConfig.END_SKY_ENABLED))
                        .option(enumOption(
                                "cirrus.config.sky.endSkyQuality",
                                CirrusConfig.END_SKY_QUALITY,
                                CirrusConfig.EndSkyQuality.class,
                                quality -> text("cirrus.config.sky.endSkyQuality." + quality.name().toLowerCase(Locale.ROOT))
                        ))
                        .option(booleanOption(
                                "cirrus.config.sky.endSkyPixelationEnabled",
                                CirrusConfig.END_SKY_PIXELATION_ENABLED
                        ))
                        .option(integerOption(
                                "cirrus.config.sky.endSkyPixelationResolution",
                                CirrusConfig.END_SKY_PIXELATION_RESOLUTION,
                                CirrusConfig.END_SKY_PIXELATION_RESOLUTION_SETTING,
                                value -> Component.literal(value + " cells")
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.endSkyIntensity",
                                CirrusConfig.END_SKY_INTENSITY,
                                CirrusConfig.END_SKY_INTENSITY_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.endSkyAnimationSpeed",
                                CirrusConfig.END_SKY_ANIMATION_SPEED,
                                CirrusConfig.END_SKY_ANIMATION_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.endSkyMorphSpeed",
                                CirrusConfig.END_SKY_MORPH_SPEED,
                                CirrusConfig.END_SKY_MORPH_SPEED_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.endSkyVoidCoverage",
                                CirrusConfig.END_SKY_VOID_COVERAGE,
                                CirrusConfig.END_SKY_VOID_COVERAGE_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.endSkyVoidDarkness",
                                CirrusConfig.END_SKY_VOID_DARKNESS,
                                CirrusConfig.END_SKY_VOID_DARKNESS_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.endSkyLightningFrequency",
                                CirrusConfig.END_SKY_LIGHTNING_FREQUENCY,
                                CirrusConfig.END_SKY_LIGHTNING_FREQUENCY_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.endSkyLightningIntensity",
                                CirrusConfig.END_SKY_LIGHTNING_INTENSITY,
                                CirrusConfig.END_SKY_LIGHTNING_INTENSITY_SETTING
                        ))
                        .option(doubleOption(
                                "cirrus.config.sky.endSkySurgeFrequency",
                                CirrusConfig.END_SKY_SURGE_FREQUENCY,
                                CirrusConfig.END_SKY_SURGE_FREQUENCY_SETTING,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption(
                                "cirrus.config.sky.endSkySurgeStrength",
                                CirrusConfig.END_SKY_SURGE_STRENGTH,
                                CirrusConfig.END_SKY_SURGE_STRENGTH_SETTING
                        ))
                        .build())
                .build();
    }

    private static Option<Boolean> booleanOption(String key, ModConfigSpec.BooleanValue value) {
        return booleanOption(key, value, description(key));
    }

    private static Option<Boolean> booleanOption(
            String key,
            ModConfigSpec.BooleanValue value,
            OptionDescription optionDescription
    ) {
        return Option.<Boolean>createBuilder()
                .name(text(key))
                .description(optionDescription)
                .binding(value.getDefault(), value::get, value::set)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static <E extends Enum<E>> Option<E> enumOption(
            String key,
            ModConfigSpec.EnumValue<E> value,
            Class<E> enumClass,
            dev.isxander.yacl3.api.controller.ValueFormatter<E> formatter
    ) {
        return Option.<E>createBuilder()
                .name(text(key))
                .description(description(key))
                .binding(value.getDefault(), value::get, value::set)
                .controller(option -> EnumControllerBuilder.create(option)
                        .enumClass(enumClass)
                        .formatValue(formatter))
                .build();
    }

    private static Option<Color> colorOption(String key, ModConfigSpec.IntValue value) {
        return Option.<Color>createBuilder()
                .name(text(key))
                .description(description(key))
                .binding(
                        new Color(value.getDefault()),
                        () -> new Color(value.get()),
                        color -> value.set(color.getRGB() & 0xFFFFFF)
                )
                .controller(ColorControllerBuilder::create)
                .build();
    }

    private static Option<Integer> integerOption(
            String key,
            ModConfigSpec.IntValue value,
            CirrusConfig.IntSetting setting,
            dev.isxander.yacl3.api.controller.ValueFormatter<Integer> formatter
    ) {
        return Option.<Integer>createBuilder()
                .name(text(key))
                .description(description(key))
                .binding(value.getDefault(), value::get, value::set)
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(setting.minimum(), setting.maximum())
                        .step(setting.step())
                        .formatValue(formatter))
                .build();
    }

    private static Option<Double> doubleOption(
            String key,
            ModConfigSpec.DoubleValue value,
            CirrusConfig.DoubleSetting setting,
            dev.isxander.yacl3.api.controller.ValueFormatter<Double> formatter
    ) {
        return Option.<Double>createBuilder()
                .name(text(key))
                .description(description(key))
                .binding(value.getDefault(), value::get, value::set)
                .controller(option -> DoubleSliderControllerBuilder.create(option)
                        .range(setting.minimum(), setting.maximum())
                        .step(setting.step())
                        .formatValue(formatter))
                .build();
    }

    private static Option<Double> percentageOption(
            String key,
            ModConfigSpec.DoubleValue value,
            CirrusConfig.DoubleSetting setting
    ) {
        return doubleOption(
                key,
                value,
                setting,
                current -> Component.literal(String.format(Locale.ROOT, "%.0f%%", current * 100.0))
        );
    }

    private static OptionDescription description(String key) {
        return OptionDescription.of(text(key + ".description"));
    }

    private static OptionDescription warningDescription(String key) {
        return OptionDescription.createBuilder()
                .text(text(key + ".description"))
                .text(text(key + ".warning").copy().withStyle(ChatFormatting.RED))
                .build();
    }

    private static Component text(String key) {
        return Component.translatable(key);
    }
}
