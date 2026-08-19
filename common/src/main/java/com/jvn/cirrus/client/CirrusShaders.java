package com.jvn.cirrus.client;

import com.jvn.cirrus.Cirrus;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

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

    public static void register(RegistrationContext context) throws IOException {
        context.register(
                Cirrus.id("cirrus_clouds"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
                shader -> clouds = shader
        );
        context.register(
                Cirrus.id("cirrus_cloud_mask"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
                shader -> cloudMask = shader
        );
        context.register(
                Cirrus.id("cirrus_sun_occlusion"),
                DefaultVertexFormat.POSITION_TEX,
                shader -> sunOcclusion = shader
        );
        context.register(
                Cirrus.id("cirrus_aurora"),
                DefaultVertexFormat.POSITION,
                shader -> aurora = shader
        );
        context.register(
                Cirrus.id("cirrus_milky_way"),
                DefaultVertexFormat.POSITION,
                shader -> milkyWay = shader
        );
        context.register(
                Cirrus.id("cirrus_stars"),
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
                shader -> stars = shader
        );
        context.register(
                Cirrus.id("cirrus_end_sky"),
                DefaultVertexFormat.POSITION,
                shader -> endSky = shader
        );
        context.register(
                Cirrus.id("cirrus_lightning_sky"),
                DefaultVertexFormat.POSITION,
                shader -> lightningSky = shader
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

    @FunctionalInterface
    public interface RegistrationContext {
        void register(
                ResourceLocation id,
                VertexFormat vertexFormat,
                Consumer<ShaderInstance> onLoaded
        ) throws IOException;
    }
}
