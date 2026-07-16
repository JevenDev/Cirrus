package com.jvn.cirrus.client;

import com.jvn.cirrus.Cirrus;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = Cirrus.MOD_ID)
public final class CirrusShaders {
    private static final ResourceLocation CLOUDS_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Cirrus.MOD_ID, "cirrus_clouds");

    private static ShaderInstance clouds;

    private CirrusShaders() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        CLOUDS_LOCATION,
                        DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
                ),
                shader -> clouds = shader
        );
    }

    public static ShaderInstance clouds() {
        return Objects.requireNonNull(clouds, "Cirrus cloud shader has not finished loading");
    }
}
