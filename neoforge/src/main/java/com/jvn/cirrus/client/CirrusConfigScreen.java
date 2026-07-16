package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import java.util.Locale;
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
                        .option(integerOption(
                                "cirrus.config.clouds.renderDistanceChunks",
                                CirrusConfig.CLOUD_RENDER_DISTANCE,
                                CirrusConfig.CLOUD_RENDER_DISTANCE_SETTING,
                                value -> Component.literal(value + " chunks")
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.lowerLayer"))
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

    private static Option<Boolean> booleanOption(String key, ModConfigSpec.BooleanValue value) {
        return Option.<Boolean>createBuilder()
                .name(text(key))
                .description(description(key))
                .binding(value.getDefault(), value::get, value::set)
                .controller(TickBoxControllerBuilder::create)
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

    private static Component text(String key) {
        return Component.translatable(key);
    }
}
