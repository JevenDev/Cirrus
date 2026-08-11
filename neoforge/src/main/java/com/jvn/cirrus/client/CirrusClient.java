package com.jvn.cirrus.client;

import com.jvn.toucanlib.neoforge.config.ToucanConfigScreens;
import net.neoforged.fml.ModContainer;

public final class CirrusClient {
    private CirrusClient() {
    }

    public static void registerConfigScreen(ModContainer modContainer) {
        ToucanConfigScreens.register(modContainer, CirrusConfigScreen::create);
    }
}
