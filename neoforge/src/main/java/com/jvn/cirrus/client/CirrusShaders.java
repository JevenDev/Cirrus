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
    private static final ResourceLocation CLOUD_MASK_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Cirrus.MOD_ID, "cirrus_cloud_mask");
    private static final ResourceLocation MOON_OCCLUSION_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Cirrus.MOD_ID, "cirrus_moon_occlusion");
    private static final ResourceLocation AURORA_LOCATION =
            ResourceLocation.fromNamespaceAndPath(Cirrus.MOD_ID, "cirrus_aurora");

    private static ShaderInstance clouds;
    private static ShaderInstance cloudMask;
    private static ShaderInstance moonOcclusion;
    private static ShaderInstance aurora;

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
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        CLOUD_MASK_LOCATION,
                        DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
                ),
                shader -> cloudMask = shader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        MOON_OCCLUSION_LOCATION,
                        DefaultVertexFormat.POSITION_TEX
                ),
                shader -> moonOcclusion = shader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        AURORA_LOCATION,
                        DefaultVertexFormat.POSITION
                ),
                shader -> aurora = shader
        );
    }

    public static ShaderInstance clouds() {
        return Objects.requireNonNull(clouds, "Cirrus cloud shader has not finished loading");
    }

    public static ShaderInstance cloudMask() {
        return Objects.requireNonNull(cloudMask, "Cirrus cloud mask shader has not finished loading");
    }

    public static ShaderInstance moonOcclusion() {
        return Objects.requireNonNull(moonOcclusion, "Cirrus moon occlusion shader has not finished loading");
    }

    public static ShaderInstance aurora() {
        return Objects.requireNonNull(aurora, "Cirrus aurora shader has not finished loading");
    }
}
