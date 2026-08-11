package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.toucanlib.client.ToucanEasing;
import com.jvn.toucanlib.client.render.ToucanSkyDome;
import com.jvn.toucanlib.neoforge.client.ToucanShaders;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import org.joml.Matrix4f;

public final class CirrusAuroraRenderer implements AutoCloseable {
    private static final float DOME_RADIUS = 100.0F;
    private static final int AZIMUTH_SEGMENTS = 64;
    private static final int ELEVATION_SEGMENTS = 20;
    private static final float MIN_ELEVATION = (float)Math.toRadians(-6.0);
    private static final float MAX_ELEVATION = (float)Math.toRadians(90.0);
    private static final int BIOME_SAMPLE_RADIUS = 24;
    private static final int[][] BIOME_SAMPLE_OFFSETS = {
            {0, 0},
            {BIOME_SAMPLE_RADIUS, 0},
            {-BIOME_SAMPLE_RADIUS, 0},
            {0, BIOME_SAMPLE_RADIUS},
            {0, -BIOME_SAMPLE_RADIUS},
            {BIOME_SAMPLE_RADIUS, BIOME_SAMPLE_RADIUS},
            {BIOME_SAMPLE_RADIUS, -BIOME_SAMPLE_RADIUS},
            {-BIOME_SAMPLE_RADIUS, BIOME_SAMPLE_RADIUS},
            {-BIOME_SAMPLE_RADIUS, -BIOME_SAMPLE_RADIUS}
    };
    private static final int[] BIOME_SAMPLE_WEIGHTS = {4, 2, 2, 2, 2, 1, 1, 1, 1};

    private VertexBuffer domeBuffer;
    private ClientLevel blendLevel;
    private float coldBiomeBlend;
    private float previousFrameTime = Float.NaN;
    private ClientLevel animationLevel;
    private float animationTime;
    private double previousAnimationGameTime = Double.NaN;
    private double previousAnimationVisualDayTime = Double.NaN;
    private boolean previousTimeTransitionActive;
    private ClientLevel variationLevel;
    private final float[] currentVariant = new float[4];
    private final float[] transitionStartVariant = new float[4];
    private final float[] transitionTargetVariant = new float[4];
    private long transitionTargetNight = Long.MIN_VALUE;
    private boolean variantInitialized;
    private boolean variantTransitionActive;

    public void render(
            ClientLevel level,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            int ticks,
            Camera camera
    ) {
        if (!CirrusConfig.AURORA_ENABLED.get()) {
            return;
        }

        float shaderTime = updateAnimationTime(level, partialTick);
        float coldStrength = CirrusConfig.AURORA_COLD_BIOMES_ONLY.get()
                ? updateColdBiomeBlend(level, partialTick, camera)
                : 1.0F;
        float nightStrength = Mth.clamp(level.getStarBrightness(partialTick) * 2.0F, 0.0F, 1.0F);
        float rain = Mth.clamp(level.getRainLevel(partialTick), 0.0F, 1.0F);
        float thunder = Mth.clamp(level.getThunderLevel(partialTick), 0.0F, 1.0F);
        float weatherVisibility = (1.0F - rain * 0.82F) * (1.0F - thunder * 0.18F);
        float intensity = coldStrength
                * nightStrength
                * weatherVisibility
                * CirrusConfig.AURORA_OPACITY.get().floatValue();
        if (intensity < 0.002F) {
            return;
        }

        prepareDome();
        ShaderInstance shader = CirrusShaders.aurora();
        ToucanShaders.setUniform(shader, "CirrusAuroraTime", shaderTime);
        ToucanShaders.setUniform(shader, "CirrusAuroraIntensity", intensity);
        ToucanShaders.setUniform(shader, "CirrusAuroraPixelation", CirrusConfig.AURORA_PIXELATION_ENABLED.get());
        ToucanShaders.setUniform(
                shader, "CirrusAuroraPixelationResolution",
                CirrusConfig.AURORA_PIXELATION_RESOLUTION.get().floatValue()
        );

        Uniform variantUniform = shader.getUniform("CirrusAuroraVariant");
        if (variantUniform != null) {
            setNightlyVariation(variantUniform, level);
        }

        ToucanShaders.setUniform(
                shader,
                "CirrusAuroraSettings",
                CirrusConfig.AURORA_MOVEMENT.get().floatValue(),
                CirrusConfig.AURORA_RIBBON_WIDTH.get().floatValue(),
                (float)Math.toRadians(CirrusConfig.AURORA_HEIGHT_DEGREES.get()),
                0.0F
        );

        float[] previousColor = RenderSystem.getShaderColor();
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        try {
            domeBuffer.bind();
            domeBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(
                    previousColor[0],
                    previousColor[1],
                    previousColor[2],
                    previousColor[3]
            );
            RenderSystem.defaultBlendFunc();
        }
    }

    private float updateColdBiomeBlend(ClientLevel level, float partialTick, Camera camera) {
        BlockPos center = BlockPos.containing(camera.getPosition());
        float target = sampledColdStrength(level, center);
        float frameTime = level.getGameTime() + partialTick;
        if (blendLevel != level || Float.isNaN(previousFrameTime)) {
            blendLevel = level;
            coldBiomeBlend = target;
            previousFrameTime = frameTime;
            return coldBiomeBlend;
        }

        float elapsedTicks = Mth.clamp(frameTime - previousFrameTime, 0.0F, 5.0F);
        previousFrameTime = frameTime;
        float response = 1.0F - (float)Math.exp(-elapsedTicks / 20.0F);
        coldBiomeBlend = Mth.lerp(response, coldBiomeBlend, target);
        return coldBiomeBlend;
    }

    private static float sampledColdStrength(ClientLevel level, BlockPos center) {
        float total = 0.0F;
        int totalWeight = 0;
        for (int index = 0; index < BIOME_SAMPLE_OFFSETS.length; index++) {
            int[] offset = BIOME_SAMPLE_OFFSETS[index];
            BlockPos samplePos = center.offset(offset[0], 0, offset[1]);
            Biome biome = level.getBiome(samplePos).value();
            float cold = Mth.clamp((0.15F - biome.getBaseTemperature()) / 0.15F, 0.0F, 1.0F);
            cold = ToucanEasing.smoothstep(cold);
            int weight = BIOME_SAMPLE_WEIGHTS[index];
            total += cold * weight;
            totalWeight += weight;
        }
        return total / totalWeight;
    }

    private void prepareDome() {
        if (domeBuffer == null) {
            domeBuffer = ToucanSkyDome.create(
                    DOME_RADIUS, AZIMUTH_SEGMENTS, ELEVATION_SEGMENTS, MIN_ELEVATION, MAX_ELEVATION
            );
        }
    }

    private float updateAnimationTime(ClientLevel level, float partialTick) {
        double gameTime = level.getGameTime() + partialTick;
        double visualDayTime = CirrusTimeTransition.visualDayTime(level, partialTick);
        if (animationLevel != level
                || Double.isNaN(previousAnimationGameTime)
                || Double.isNaN(previousAnimationVisualDayTime)) {
            animationLevel = level;
            animationTime = 0.0F;
            previousAnimationGameTime = gameTime;
            previousAnimationVisualDayTime = visualDayTime;
            return animationTime;
        }

        double gameTimeDelta = Math.max(0.0, gameTime - previousAnimationGameTime);
        double visualTimeDelta = Math.max(0.0, visualDayTime - previousAnimationVisualDayTime);
        previousAnimationGameTime = gameTime;
        previousAnimationVisualDayTime = visualDayTime;

        // Preserve ordinary animation when daylight is frozen. Smooth time
        // transitions contribute only a twelfth of their extra acceleration so
        // the aurora reacts without racing through several shapes at once.
        double acceleratedExtra = Math.max(0.0, visualTimeDelta - gameTimeDelta);
        boolean timeTransitionActive = CirrusTimeTransition.isTransitionActive();
        double transitionScale = timeTransitionActive || previousTimeTransitionActive
                ? 1.0 / 12.0
                : 1.0;
        previousTimeTransitionActive = timeTransitionActive;
        double elapsedTicks = Math.min(gameTimeDelta + acceleratedExtra * transitionScale, 1200.0);
        animationTime += (float)(elapsedTicks
                * CirrusConfig.AURORA_ANIMATION_SPEED.get()
                / 20.0);
        return animationTime;
    }

    private void setNightlyVariation(Uniform uniform, ClientLevel level) {
        long visualNight = Math.floorDiv(CirrusTimeTransition.visualDayTime(), 24000L);
        if (variationLevel != level) {
            variationLevel = level;
            variantInitialized = false;
            variantTransitionActive = false;
            transitionTargetNight = Long.MIN_VALUE;
        }
        if (!variantInitialized) {
            fillVariant(level, visualNight, currentVariant);
            variantInitialized = true;
        }

        if (CirrusTimeTransition.isTransitionActive()) {
            long targetNight = Math.floorDiv(CirrusTimeTransition.authoritativeDayTime(), 24000L);
            if (!variantTransitionActive || transitionTargetNight != targetNight) {
                System.arraycopy(currentVariant, 0, transitionStartVariant, 0, currentVariant.length);
                fillVariant(level, targetNight, transitionTargetVariant);
                transitionTargetNight = targetNight;
                variantTransitionActive = true;
            }

            float progress = (float)CirrusTimeTransition.transitionProgress();
            for (int index = 0; index < currentVariant.length; index++) {
                currentVariant[index] = Mth.lerp(
                        progress,
                        transitionStartVariant[index],
                        transitionTargetVariant[index]
                );
            }
        } else {
            // Recompute from the settled visual night so the final transition
            // frame and all subsequent frames use the exact same layout.
            fillVariant(level, visualNight, currentVariant);
            variantTransitionActive = false;
            transitionTargetNight = Long.MIN_VALUE;
        }

        float strength = CirrusConfig.AURORA_NIGHTLY_VARIATION.get().floatValue();
        uniform.set(
                Mth.lerp(strength, 0.5F, currentVariant[0]),
                Mth.lerp(strength, 0.5F, currentVariant[1]),
                Mth.lerp(strength, 0.5F, currentVariant[2]),
                Mth.lerp(strength, 0.5F, currentVariant[3])
        );
    }

    private static void fillVariant(ClientLevel level, long night, float[] variant) {
        long dimensionSalt = (long)level.dimension().location().hashCode() * 0x9E3779B97F4A7C15L;
        long key = night ^ dimensionSalt;
        variant[0] = variationValue(key, 0x243F6A8885A308D3L);
        variant[1] = variationValue(key, 0x13198A2E03707344L);
        variant[2] = variationValue(key, 0xA4093822299F31D0L);
        variant[3] = variationValue(key, 0x082EFA98EC4E6C89L);
    }

    private static float variationValue(long key, long salt) {
        long mixed = key + salt;
        mixed = (mixed ^ mixed >>> 30) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return (mixed >>> 40) / 16777216.0F;
    }

    public void invalidate() {
        blendLevel = null;
        coldBiomeBlend = 0.0F;
        previousFrameTime = Float.NaN;
        animationLevel = null;
        animationTime = 0.0F;
        previousAnimationGameTime = Double.NaN;
        previousAnimationVisualDayTime = Double.NaN;
        previousTimeTransitionActive = false;
        variationLevel = null;
        variantInitialized = false;
        variantTransitionActive = false;
        transitionTargetNight = Long.MIN_VALUE;
    }

    @Override
    public void close() {
        if (domeBuffer != null) {
            domeBuffer.close();
            domeBuffer = null;
        }
        invalidate();
    }
}
