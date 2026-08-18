package com.jvn.cirrus.client;

import com.jvn.cirrus.Cirrus;
import com.jvn.toucanlib.neoforge.client.ToucanShaders;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.Objects;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = Cirrus.MOD_ID)
public final class CirrusShaders {
    private static ShaderInstance clouds;
    private static ShaderInstance cloudMask;
    private static ShaderInstance sunOcclusion;
    private static ShaderInstance aurora;
    private static ShaderInstance milkyWay;
    private static ShaderInstance stars;
    private static ShaderInstance endSky;
    private static ShaderInstance lightningSky;

    private CirrusShaders() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        ToucanShaders.register(
                event, Cirrus.IDS.id("cirrus_clouds"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, shader -> clouds = shader
        );
        ToucanShaders.register(
                event, Cirrus.IDS.id("cirrus_cloud_mask"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, shader -> cloudMask = shader
        );
        ToucanShaders.register(
                event, Cirrus.IDS.id("cirrus_sun_occlusion"),
                DefaultVertexFormat.POSITION_TEX, shader -> sunOcclusion = shader
        );
        ToucanShaders.register(
                event, Cirrus.IDS.id("cirrus_aurora"),
                DefaultVertexFormat.POSITION, shader -> aurora = shader
        );
        ToucanShaders.register(
                event, Cirrus.IDS.id("cirrus_milky_way"),
                DefaultVertexFormat.POSITION, shader -> milkyWay = shader
        );
        ToucanShaders.register(
                event, Cirrus.IDS.id("cirrus_stars"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, shader -> stars = shader
        );
        ToucanShaders.register(
                event, Cirrus.IDS.id("cirrus_end_sky"),
                DefaultVertexFormat.POSITION, shader -> endSky = shader
        );
        ToucanShaders.register(
                event, Cirrus.IDS.id("cirrus_lightning_sky"),
                DefaultVertexFormat.POSITION, shader -> lightningSky = shader
        );
    }

    public static ShaderInstance clouds() {
        return Objects.requireNonNull(clouds, "Cirrus cloud shader has not finished loading");
    }

    public static ShaderInstance cloudMask() {
        return Objects.requireNonNull(cloudMask, "Cirrus cloud mask shader has not finished loading");
    }

    public static ShaderInstance sunOcclusion() {
        return Objects.requireNonNull(sunOcclusion, "Cirrus sun occlusion shader has not finished loading");
    }

    public static ShaderInstance aurora() {
        return Objects.requireNonNull(aurora, "Cirrus aurora shader has not finished loading");
    }

    public static ShaderInstance milkyWay() {
        return Objects.requireNonNull(milkyWay, "Cirrus Milky Way shader has not finished loading");
    }

    public static ShaderInstance stars() {
        return Objects.requireNonNull(stars, "Cirrus star shader has not finished loading");
    }

    public static ShaderInstance endSky() {
        return Objects.requireNonNull(endSky, "Cirrus End sky shader has not finished loading");
    }

    public static ShaderInstance lightningSky() {
        return Objects.requireNonNull(lightningSky, "Cirrus lightning sky shader has not finished loading");
    }
}
