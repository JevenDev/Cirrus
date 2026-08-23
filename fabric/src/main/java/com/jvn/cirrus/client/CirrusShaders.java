package com.jvn.cirrus.client;

import static com.jvn.cirrus.client.render.CirrusShader.uniform;

import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.client.render.CirrusShader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class CirrusShaders {
    private static final CirrusShader CLOUDS = new CirrusShader(
            "cirrus_clouds",
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
            CirrusShader.Blend.TRANSLUCENT,
            CirrusShader.Target.CLOUDS,
            Identifier.withDefaultNamespace("textures/environment/clouds.png"),
            true,
            true,
            true,
            uniform("ColorModulator", 4, 1.0F, 1.0F, 1.0F, 1.0F),
            uniform("CirrusEnabled", 1, 0.0F),
            uniform("CirrusLightDirection", 2, 0.0F, 0.0F),
            uniform("CirrusSunWeight", 1, 1.0F),
            uniform("CirrusLightViewDirection", 4, 0.0F, 1.0F, 0.0F, 0.0F),
            uniform("CirrusRainLevel", 1, 0.0F),
            uniform("CirrusThunderLevel", 1, 0.0F),
            uniform("CirrusRainCloudCoverage", 1, 0.0F),
            uniform("CirrusThunderCloudCoverage", 1, 0.0F),
            uniform("CirrusLightningFlash", 1, 0.0F),
            uniform("CirrusLightningViewPosition", 4, 0.0F, 0.0F, 0.0F, 0.0F),
            uniform("CirrusWorldUpViewDirection", 4, 0.0F, 1.0F, 0.0F, 0.0F),
            uniform("CirrusLightningRadius", 1, 640.0F)
    );
    private static final CirrusShader CLOUD_MASK = new CirrusShader(
            "cirrus_cloud_mask",
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
            CirrusShader.Blend.NONE,
            CirrusShader.Target.MAIN,
            Identifier.withDefaultNamespace("textures/environment/clouds.png"),
            false,
            true,
            uniform("CirrusRainCloudCoverage", 1, 0.0F),
            uniform("CirrusThunderCloudCoverage", 1, 0.0F),
            uniform("CirrusLayerOpacity", 1, 1.0F)
    );
    private static final CirrusShader AURORA = new CirrusShader(
            "cirrus_aurora",
            DefaultVertexFormat.POSITION,
            CirrusShader.Blend.ADDITIVE,
            CirrusShader.Target.MAIN,
            Cirrus.texture("environment/aurora_noise.png"),
            true,
            false,
            uniform("CirrusAuroraTime", 1, 0.0F),
            uniform("CirrusAuroraIntensity", 1, 0.0F),
            uniform("CirrusAuroraPixelation", 1, 1.0F),
            uniform("CirrusAuroraPixelationResolution", 1, 320.0F),
            uniform("CirrusAuroraVariant", 4, 0.5F, 0.5F, 0.5F, 0.5F),
            uniform("CirrusAuroraSettings", 4, 1.25F, 1.0F, 0.0F, 0.0F)
    );
    private static final CirrusShader MILKY_WAY = new CirrusShader(
            "cirrus_milky_way",
            DefaultVertexFormat.POSITION,
            CirrusShader.Blend.TRANSLUCENT,
            CirrusShader.Target.MAIN,
            Cirrus.texture("environment/milky_way_lookup.png"),
            true,
            false,
            uniform("CirrusMilkyWayIntensity", 1, 0.0F),
            uniform("CirrusMilkyWayPixelation", 1, 1.0F),
            uniform("CirrusMilkyWayPixelationResolution", 1, 320.0F),
            uniform("CirrusSkyGradientIntensity", 1, 0.0F),
            uniform("CirrusSkyGradientHeight", 1, 0.55F),
            uniform("CirrusMilkyWayRotation", 1, 0.0F),
            uniform("CirrusSkyHorizonColor", 3, 1.0F, 0.62F, 0.47F),
            uniform("CirrusSkyZenithColor", 3, 0.45F, 0.59F, 0.80F)
    );
    private static final CirrusShader STARS = new CirrusShader(
            "cirrus_stars",
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
            CirrusShader.Blend.TRANSLUCENT,
            CirrusShader.Target.MAIN,
            Cirrus.texture("environment/north_star.png"),
            true,
            false,
            uniform("CirrusStarAppearance", 4, 1.0F, 0.25F, 1.0F, 1.0F),
            uniform("CirrusStarAnimation", 4, 0.55F, 2.0F, 0.0F, 1.0F),
            uniform("CirrusStarAtmosphere", 2, 0.0F, 1.0F),
            uniform("CirrusShootingStarAppearance", 4, 1.0F, 0.75F, 1.5F, 0.85F),
            uniform("CirrusShootingStarAnimation", 4, 0.0F, 1.0F, 0.65F, 1.35F),
            uniform("CirrusShootingStarDynamics", 2, 0.65F, 0.0F),
            uniform("CirrusShootingStarVisual", 4, 3.0F, 2.0F, 1.0F, 0.0F),
            uniform("CirrusStarRenderMode", 1, 0.0F)
    );
    private static final CirrusShader END_SKY = new CirrusShader(
            "cirrus_end_sky",
            DefaultVertexFormat.POSITION,
            CirrusShader.Blend.TRANSLUCENT,
            CirrusShader.Target.MAIN,
            null,
            true,
            false,
            uniform("CirrusEndNoiseOctaves", 1, 3.0F),
            uniform("CirrusEndTime", 1, 0.0F),
            uniform("CirrusEndIntensity", 1, 1.0F),
            uniform("CirrusEndAnimationSpeed", 1, 1.0F),
            uniform("CirrusEndMorphSpeed", 1, 1.0F),
            uniform("CirrusEndVoidCoverage", 1, 1.0F),
            uniform("CirrusEndVoidDarkness", 1, 1.0F),
            uniform("CirrusEndLightningFrequency", 1, 1.0F),
            uniform("CirrusEndLightningIntensity", 1, 1.0F),
            uniform("CirrusEndSurgeFrequency", 1, 1.0F),
            uniform("CirrusEndSurgeStrength", 1, 1.0F),
            uniform("CirrusEndPixelationResolution", 1, 480.0F),
            uniform("CirrusEndPixelation", 1, 1.0F)
    );
    private static final CirrusShader LIGHTNING_SKY = new CirrusShader(
            "cirrus_lightning_sky",
            DefaultVertexFormat.POSITION,
            CirrusShader.Blend.ADDITIVE,
            CirrusShader.Target.MAIN,
            null,
            true,
            false,
            uniform("CirrusLightningSkyDirection", 3, 0.0F, 1.0F, 0.0F),
            uniform("CirrusLightningSkyIntensity", 1, 0.0F)
    );

    private static boolean samplerTexturesLoaded;

    private CirrusShaders() {
    }

    public static void initialize() {
        // Forces pipeline registration during client initialization.
    }

    public static void preloadSamplerTextures(Minecraft minecraft) {
        if (samplerTexturesLoaded) {
            return;
        }

        minecraft.getTextureManager().getTexture(
                Identifier.withDefaultNamespace("textures/environment/clouds.png")
        );
        minecraft.getTextureManager().getTexture(Cirrus.texture("environment/aurora_noise.png"));
        minecraft.getTextureManager().getTexture(Cirrus.texture("environment/milky_way_lookup.png"));
        minecraft.getTextureManager().getTexture(Cirrus.texture("environment/north_star.png"));
        samplerTexturesLoaded = true;
    }

    public static CirrusShader clouds() {
        return CLOUDS;
    }

    public static CirrusShader cloudMask() {
        return CLOUD_MASK;
    }

    public static CirrusShader aurora() {
        return AURORA;
    }

    public static CirrusShader milkyWay() {
        return MILKY_WAY;
    }

    public static CirrusShader stars() {
        return STARS;
    }

    public static CirrusShader endSky() {
        return END_SKY;
    }

    public static CirrusShader lightningSky() {
        return LIGHTNING_SKY;
    }
}
