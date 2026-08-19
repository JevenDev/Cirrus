package com.jvn.cirrus.fabric;

import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.client.CirrusLightningRenderer;
import com.jvn.cirrus.client.CirrusShaders;
import net.fabricmc.api.ClientModInitializer;

public final class CirrusFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Cirrus.init();
        CirrusLightningRenderer.initialize();
        CirrusShaders.initialize();
    }
}
