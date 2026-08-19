package com.jvn.cirrus.fabric;

import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.client.CirrusShaders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;

public final class CirrusFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Cirrus.init();
        CoreShaderRegistrationCallback.EVENT.register(context ->
                CirrusShaders.register(context::register)
        );
    }
}
