package com.jvn.cirrus;

import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.cirrus.client.CirrusClient;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

@Mod(Cirrus.MOD_ID)
public final class Cirrus {
    public static final String MOD_ID = "cirrus";

    public Cirrus(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, CirrusConfig.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CirrusClient.registerConfigScreen(modContainer);
        }
    }
}
