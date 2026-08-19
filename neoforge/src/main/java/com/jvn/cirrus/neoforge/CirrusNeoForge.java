package com.jvn.cirrus.neoforge;

import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.client.CirrusConfigScreen;
import com.jvn.cirrus.client.CirrusShaders;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.io.IOException;

@Mod(Cirrus.MOD_ID)
public final class CirrusNeoForge {
    public CirrusNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        Cirrus.init();
        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (container, parent) -> CirrusConfigScreen.create(parent)
        );
        modEventBus.addListener(CirrusNeoForge::registerShaders);
    }

    private static void registerShaders(RegisterShadersEvent event) {
        try {
            CirrusShaders.register((id, vertexFormat, onLoaded) ->
                    event.registerShader(
                            new ShaderInstance(event.getResourceProvider(), id, vertexFormat),
                            onLoaded
                    )
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to register Cirrus shaders", exception);
        }
    }
}
