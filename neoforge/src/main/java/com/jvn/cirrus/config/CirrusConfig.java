package com.jvn.cirrus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CirrusConfig {
    public static final ModConfigSpec.IntValue CLOUD_RENDER_DISTANCE;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Cloud rendering").push("clouds");
        CLOUD_RENDER_DISTANCE = builder
                .comment("Cloud render distance in chunks, independent of terrain render distance.")
                .defineInRange("renderDistanceChunks", 128, 2, 128);
        builder.pop();
        SPEC = builder.build();
    }

    private CirrusConfig() {
    }
}
