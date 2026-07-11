package com.jvn.examplemod.common;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class ExampleModCommon {
    public static final String MOD_ID = "examplemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    private ExampleModCommon() {
    }

    public static void init() {
        LOGGER.info("{} common scaffolding loaded.", MOD_ID);
    }
}
