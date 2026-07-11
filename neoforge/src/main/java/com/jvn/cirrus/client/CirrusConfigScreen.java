package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.cirrus.config.CloudQuality;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
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
                        .option(integerOption(
                                "cirrus.config.clouds.renderDistanceChunks",
                                CirrusConfig.CLOUD_RENDER_DISTANCE,
                                2,
                                128,
                                1,
                                value -> Component.literal(value + " chunks")
                        ))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.lowerLayer"))
                        .option(qualityOption("cirrus.config.clouds.lowerLayerQuality", CirrusConfig.LOWER_LAYER_QUALITY))
                        .option(doubleOption(
                                "cirrus.config.clouds.lowerLayerHeightOffset",
                                CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET,
                                -128.0,
                                128.0,
                                1.0,
                                value -> Component.literal(String.format(Locale.ROOT, "%.0f blocks", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.clouds.lowerLayerSpeed",
                                CirrusConfig.LOWER_LAYER_SPEED,
                                0.0,
                                4.0,
                                0.05,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption("cirrus.config.clouds.lowerLayerOpacity", CirrusConfig.LOWER_LAYER_OPACITY, 0.05))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(text("cirrus.config.group.upperLayer"))
                        .option(booleanOption("cirrus.config.clouds.upperLayerEnabled", CirrusConfig.UPPER_LAYER_ENABLED))
                        .option(qualityOption("cirrus.config.clouds.upperLayerQuality", CirrusConfig.UPPER_LAYER_QUALITY))
                        .option(doubleOption(
                                "cirrus.config.clouds.upperLayerHeightOffset",
                                CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET,
                                16.0,
                                256.0,
                                1.0,
                                value -> Component.literal(String.format(Locale.ROOT, "%.0f blocks", value))
                        ))
                        .option(doubleOption(
                                "cirrus.config.clouds.upperLayerSpeed",
                                CirrusConfig.UPPER_LAYER_SPEED,
                                0.0,
                                4.0,
                                0.05,
                                value -> Component.literal(String.format(Locale.ROOT, "%.2fx", value))
                        ))
                        .option(percentageOption("cirrus.config.clouds.upperLayerOpacity", CirrusConfig.UPPER_LAYER_OPACITY, 0.05))
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
            int minimum,
            int maximum,
            int step,
            dev.isxander.yacl3.api.controller.ValueFormatter<Integer> formatter
    ) {
        return Option.<Integer>createBuilder()
                .name(text(key))
                .description(description(key))
                .binding(value.getDefault(), value::get, value::set)
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(minimum, maximum)
                        .step(step)
                        .formatValue(formatter))
                .build();
    }

    private static Option<Double> doubleOption(
            String key,
            ModConfigSpec.DoubleValue value,
            double minimum,
            double maximum,
            double step,
            dev.isxander.yacl3.api.controller.ValueFormatter<Double> formatter
    ) {
        return Option.<Double>createBuilder()
                .name(text(key))
                .description(description(key))
                .binding(value.getDefault(), value::get, value::set)
                .controller(option -> DoubleSliderControllerBuilder.create(option)
                        .range(minimum, maximum)
                        .step(step)
                        .formatValue(formatter))
                .build();
    }

    private static Option<Double> percentageOption(
            String key,
            ModConfigSpec.DoubleValue value,
            double minimum
    ) {
        return doubleOption(
                key,
                value,
                minimum,
                1.0,
                0.05,
                current -> Component.literal(String.format(Locale.ROOT, "%.0f%%", current * 100.0))
        );
    }

    private static Option<CloudQuality> qualityOption(
            String key,
            ModConfigSpec.EnumValue<CloudQuality> value
    ) {
        return Option.<CloudQuality>createBuilder()
                .name(text(key))
                .description(description(key))
                .binding(value.getDefault(), value::get, value::set)
                .controller(option -> EnumControllerBuilder.create(option)
                        .enumClass(CloudQuality.class)
                        .formatValue(quality -> text(
                                "cirrus.config.quality." + quality.name().toLowerCase(Locale.ROOT)
                        )))
                .build();
    }

    private static OptionDescription description(String key) {
        return OptionDescription.of(text(key + ".description"));
    }

    private static Component text(String key) {
        return Component.translatable(key);
    }
}
