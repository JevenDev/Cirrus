package com.jvn.examplemod.client;

import com.jvn.examplemod.ExampleMod;
import net.fabricmc.api.ClientModInitializer;

public final class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ExampleMod.LOGGER.info("{} client scaffolding loaded.", ExampleMod.MOD_ID);
    }
}
