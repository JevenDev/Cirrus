package com.jvn.cirrus.fabric;

import com.jvn.cirrus.client.CirrusConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class CirrusModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CirrusConfigScreen::create;
    }
}
