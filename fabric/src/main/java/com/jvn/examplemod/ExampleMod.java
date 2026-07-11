package com.jvn.examplemod;

import com.jvn.examplemod.common.ExampleModCommon;
import net.fabricmc.api.ModInitializer;

public final class ExampleMod implements ModInitializer {
    public static final String MOD_ID = ExampleModCommon.MOD_ID;
    public static final org.slf4j.Logger LOGGER = ExampleModCommon.LOGGER;

    @Override
    public void onInitialize() {
        ExampleModCommon.init();
    }
}
