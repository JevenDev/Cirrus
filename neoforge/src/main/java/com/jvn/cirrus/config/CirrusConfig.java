package com.jvn.cirrus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CirrusConfig {
    public static final ModConfigSpec.IntValue CLOUD_RENDER_DISTANCE;
    public static final ModConfigSpec.BooleanValue UPPER_LAYER_ENABLED;
    public static final ModConfigSpec.DoubleValue UPPER_LAYER_HEIGHT_OFFSET;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Cloud rendering").push("clouds");
        CLOUD_RENDER_DISTANCE = builder
                .comment("Cloud render distance in chunks, independent of terrain render distance.")
                .defineInRange("renderDistanceChunks", 128, 2, 128);
        UPPER_LAYER_ENABLED = builder
                .comment("Render a second, independently sampled cloud layer above the dimension's normal clouds.")
                .define("upperLayerEnabled", true);
        UPPER_LAYER_HEIGHT_OFFSET = builder
                .comment("Upper cloud layer height above the normal cloud layer, in blocks.")
                .defineInRange("upperLayerHeightOffset", 64.0, 16.0, 256.0);
        builder.pop();
        SPEC = builder.build();
    }

    private CirrusConfig() {
    }
}
