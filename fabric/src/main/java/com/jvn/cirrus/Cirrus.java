package com.jvn.cirrus;

import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Cirrus {
    public static final String MOD_ID = "cirrus";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private Cirrus() {
    }

    public static void init() {
        if (INITIALIZED.compareAndSet(false, true)) {
            CirrusConfig.SPEC.load(FabricLoader.getInstance().getConfigDir().resolve("cirrus-client.json"));
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static Identifier texture(String path) {
        return id("textures/" + path);
    }
}
