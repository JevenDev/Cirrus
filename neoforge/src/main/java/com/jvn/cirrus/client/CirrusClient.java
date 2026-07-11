package com.jvn.cirrus.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class CirrusClient {
    private CirrusClient() {
    }

    public static void registerConfigScreen(ModContainer modContainer) {
        IConfigScreenFactory factory = (container, parent) -> new CirrusConfigScreen(parent);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
