package com.jvn.examplemod;

import com.jvn.examplemod.common.ExampleModCommon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ExampleMod.MOD_ID)
public final class ExampleMod {
    public static final String MOD_ID = ExampleModCommon.MOD_ID;
    public static final Logger LOGGER = ExampleModCommon.LOGGER;

    public ExampleMod(IEventBus modEventBus, ModContainer modContainer) {
        ExampleModCommon.init();
    }
}
