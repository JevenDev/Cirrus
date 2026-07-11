package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.cirrus.config.CloudQuality;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public final class CirrusConfigScreen extends OptionsSubScreen {
    private final OptionInstance<Integer> cloudDistance = integerSlider(
            "cirrus.config.clouds.renderDistanceChunks",
            2,
            128,
            CirrusConfig.CLOUD_RENDER_DISTANCE.get(),
            CirrusConfig.CLOUD_RENDER_DISTANCE::set
    );
    private final OptionInstance<CloudQuality> lowerQuality = qualityOption(
            "cirrus.config.clouds.lowerLayerQuality",
            CirrusConfig.LOWER_LAYER_QUALITY.get(),
            CirrusConfig.LOWER_LAYER_QUALITY::set
    );
    private final OptionInstance<Double> lowerHeight = doubleSlider(
            "cirrus.config.clouds.lowerLayerHeightOffset",
            -128.0,
            128.0,
            CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get(),
            CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET::set,
            "%.0f blocks"
    );
    private final OptionInstance<Double> lowerSpeed = doubleSlider(
            "cirrus.config.clouds.lowerLayerSpeed",
            0.0,
            4.0,
            CirrusConfig.LOWER_LAYER_SPEED.get(),
            CirrusConfig.LOWER_LAYER_SPEED::set,
            "%.2fx"
    );
    private final OptionInstance<Double> lowerOpacity = percentageSlider(
            "cirrus.config.clouds.lowerLayerOpacity",
            CirrusConfig.LOWER_LAYER_OPACITY.get(),
            CirrusConfig.LOWER_LAYER_OPACITY::set
    );
    private final OptionInstance<Boolean> upperEnabled = booleanOption(
            "cirrus.config.clouds.upperLayerEnabled",
            CirrusConfig.UPPER_LAYER_ENABLED.get(),
            CirrusConfig.UPPER_LAYER_ENABLED::set
    );
    private final OptionInstance<CloudQuality> upperQuality = qualityOption(
            "cirrus.config.clouds.upperLayerQuality",
            CirrusConfig.UPPER_LAYER_QUALITY.get(),
            CirrusConfig.UPPER_LAYER_QUALITY::set
    );
    private final OptionInstance<Double> upperHeight = doubleSlider(
            "cirrus.config.clouds.upperLayerHeightOffset",
            16.0,
            256.0,
            CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get(),
            CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET::set,
            "%.0f blocks"
    );
    private final OptionInstance<Double> upperSpeed = doubleSlider(
            "cirrus.config.clouds.upperLayerSpeed",
            0.0,
            4.0,
            CirrusConfig.UPPER_LAYER_SPEED.get(),
            CirrusConfig.UPPER_LAYER_SPEED::set,
            "%.2fx"
    );
    private final OptionInstance<Double> upperOpacity = percentageSlider(
            "cirrus.config.clouds.upperLayerOpacity",
            CirrusConfig.UPPER_LAYER_OPACITY.get(),
            CirrusConfig.UPPER_LAYER_OPACITY::set
    );

    public CirrusConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("cirrus.config.title"));
    }

    @Override
    protected void addOptions() {
        list.addSmall(cloudDistance, upperEnabled);
        list.addSmall(lowerQuality, upperQuality);
        list.addSmall(lowerHeight, upperHeight);
        list.addSmall(lowerSpeed, upperSpeed);
        list.addSmall(lowerOpacity, upperOpacity);
    }

    @Override
    public void removed() {
        super.removed();
        CirrusConfig.SPEC.save();
    }

    private static OptionInstance<Boolean> booleanOption(String key, boolean value, Consumer<Boolean> setter) {
        return OptionInstance.createBoolean(key, value, setter);
    }

    private static OptionInstance<Integer> integerSlider(
            String key,
            int minimum,
            int maximum,
            int value,
            Consumer<Integer> setter
    ) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                Options::genericValueLabel,
                new OptionInstance.IntRange(minimum, maximum),
                value,
                setter
        );
    }

    private static OptionInstance<Double> doubleSlider(
            String key,
            double minimum,
            double maximum,
            double value,
            Consumer<Double> setter,
            String format
    ) {
        return doubleSlider(key, minimum, maximum, value, setter, format, 1.0);
    }

    private static OptionInstance<Double> percentageSlider(
            String key,
            double value,
            Consumer<Double> setter
    ) {
        return percentageSlider(key, 0.05, value, setter);
    }

    private static OptionInstance<Double> percentageSlider(
            String key,
            double minimum,
            double value,
            Consumer<Double> setter
    ) {
        return doubleSlider(key, minimum, 1.0, value, setter, "%.0f%%", 100.0);
    }

    private static OptionInstance<Double> doubleSlider(
            String key,
            double minimum,
            double maximum,
            double value,
            Consumer<Double> setter,
            String format,
            double displayMultiplier
    ) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, current) -> Options.genericValueLabel(
                        caption,
                        Component.literal(String.format(Locale.ROOT, format, current * displayMultiplier))
                ),
                OptionInstance.UnitDouble.INSTANCE.xmap(
                        slider -> minimum + slider * (maximum - minimum),
                        current -> (current - minimum) / (maximum - minimum)
                ),
                value,
                setter
        );
    }

    private static OptionInstance<CloudQuality> qualityOption(
            String key,
            CloudQuality value,
            Consumer<CloudQuality> setter
    ) {
        return new OptionInstance<>(
                key,
                OptionInstance.noTooltip(),
                (caption, quality) -> Options.genericValueLabel(
                        caption,
                        Component.translatable("cirrus.config.quality." + quality.name().toLowerCase(Locale.ROOT))
                ),
                new OptionInstance.Enum<>(
                        List.of(CloudQuality.values()),
                        Codec.STRING.xmap(CloudQuality::valueOf, CloudQuality::name)
                ),
                value,
                setter
        );
    }
}
