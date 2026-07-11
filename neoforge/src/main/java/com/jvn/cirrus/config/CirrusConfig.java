package com.jvn.cirrus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CirrusConfig {
    public static final ModConfigSpec SPEC = new ModConfigSpec.Builder()
            .comment("Cirrus client-side rendering settings")
            .build();

    private CirrusConfig() {
    }
}
