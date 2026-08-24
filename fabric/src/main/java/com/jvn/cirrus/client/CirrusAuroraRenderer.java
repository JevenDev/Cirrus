package com.jvn.cirrus.client;

import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.cirrus.client.util.CirrusEasing;
import com.jvn.cirrus.client.util.CirrusSkyDome;
import com.jvn.cirrus.client.util.CirrusShaderUniforms;
import com.jvn.cirrus.client.render.CirrusUniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.jvn.cirrus.client.render.CirrusVertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import com.jvn.cirrus.client.render.CirrusShader;
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
    private static final int BIOME_SAMPLE_INTERVAL_TICKS = 20;
    private static final int BIOME_SAMPLE_MOVEMENT_THRESHOLD = 8;
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

    private CirrusVertexBuffer domeBuffer;
    private ClientLevel blendLevel;
    private float coldBiomeBlend;
    private double previousFrameTime = Double.NaN;
    private ClientLevel sampledBiomeLevel;
    private int lastBiomeSampleTick;
    private int sampledBiomeX;
    private int sampledBiomeY;
    private int sampledBiomeZ;
    private float sampledColdStrength;
    private ClientLevel animationLevel;
    private float animationTime;
    private double previousAnimationRenderTime = Double.NaN;
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

        float shaderTime = updateAnimationTime(level, partialTick, ticks);
        float nightStrength = Mth.clamp(CirrusRenderContext.starBrightness(level, partialTick) * 2.0F, 0.0F, 1.0F);
        float rain = Mth.clamp(level.getRainLevel(partialTick), 0.0F, 1.0F);
        float thunder = Mth.clamp(level.getThunderLevel(partialTick), 0.0F, 1.0F);
        float weatherVisibility = (1.0F - rain * 0.82F) * (1.0F - thunder * 0.18F);
        float visibleIntensity = nightStrength
                * weatherVisibility
                * CirrusConfig.AURORA_OPACITY.get().floatValue();
        if (visibleIntensity < 0.002F) {
            return;
        }
        float coldStrength = CirrusConfig.AURORA_COLD_BIOMES_ONLY.get()
                ? updateColdBiomeBlend(level, partialTick, ticks, camera)
                : 1.0F;
        float intensity = coldStrength * visibleIntensity;
        if (intensity < 0.002F) {
            return;
        }

        prepareDome();
        CirrusShader shader = CirrusShaders.aurora();
        CirrusShaderUniforms.setUniform(shader, "CirrusAuroraTime", shaderTime);
        CirrusShaderUniforms.setUniform(shader, "CirrusAuroraIntensity", intensity);
        CirrusShaderUniforms.setUniform(shader, "CirrusAuroraPixelation", CirrusConfig.AURORA_PIXELATION_ENABLED.get());
        CirrusShaderUniforms.setUniform(
                shader, "CirrusAuroraPixelationResolution",
                CirrusConfig.AURORA_PIXELATION_RESOLUTION.get().floatValue()
        );

        CirrusUniform variantUniform = shader.getUniform("CirrusAuroraVariant");
        if (variantUniform != null) {
            setNightlyVariation(variantUniform, level);
        }

        CirrusShaderUniforms.setUniform(
                shader,
                "CirrusAuroraSettings",
                CirrusConfig.AURORA_MOVEMENT.get().floatValue(),
                CirrusConfig.AURORA_RIBBON_WIDTH.get().floatValue(),
                (float)Math.toRadians(CirrusConfig.AURORA_HEIGHT_DEGREES.get()),
                0.0F
        );

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        domeBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
    }

    private float updateColdBiomeBlend(ClientLevel level, float partialTick, int ticks, Camera camera) {
        int centerX = Mth.floor(camera.position().x);
        int centerY = Mth.floor(camera.position().y);
        int centerZ = Mth.floor(camera.position().z);
        boolean movedOutsideSampleArea = Math.abs(centerX - sampledBiomeX) >= BIOME_SAMPLE_MOVEMENT_THRESHOLD
                || Math.abs(centerY - sampledBiomeY) >= BIOME_SAMPLE_MOVEMENT_THRESHOLD
                || Math.abs(centerZ - sampledBiomeZ) >= BIOME_SAMPLE_MOVEMENT_THRESHOLD;
        if (sampledBiomeLevel != level
                || ticks - lastBiomeSampleTick >= BIOME_SAMPLE_INTERVAL_TICKS
                || movedOutsideSampleArea) {
            sampledBiomeLevel = level;
            lastBiomeSampleTick = ticks;
            sampledBiomeX = centerX;
            sampledBiomeY = centerY;
            sampledBiomeZ = centerZ;
            sampledColdStrength = sampledColdStrength(level, centerX, centerY, centerZ);
        }

        double frameTime = ticks + (double)partialTick;
        if (blendLevel != level || Double.isNaN(previousFrameTime)) {
            blendLevel = level;
            coldBiomeBlend = sampledColdStrength;
            previousFrameTime = frameTime;
            return coldBiomeBlend;
        }

        double elapsedTicks = Math.max(frameTime - previousFrameTime, 0.0);
        previousFrameTime = frameTime;
        if (elapsedTicks > BIOME_SAMPLE_INTERVAL_TICKS) {
            coldBiomeBlend = sampledColdStrength;
            return coldBiomeBlend;
        }
        elapsedTicks = Math.min(elapsedTicks, 5.0);
        float response = 1.0F - (float)Math.exp(-elapsedTicks / 20.0);
        coldBiomeBlend = Mth.lerp(response, coldBiomeBlend, sampledColdStrength);
        return coldBiomeBlend;
    }

    private static float sampledColdStrength(ClientLevel level, int centerX, int centerY, int centerZ) {
        float total = 0.0F;
        int totalWeight = 0;
        BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
        for (int index = 0; index < BIOME_SAMPLE_OFFSETS.length; index++) {
            int[] offset = BIOME_SAMPLE_OFFSETS[index];
            samplePos.set(centerX + offset[0], centerY, centerZ + offset[1]);
            Biome biome = level.getBiome(samplePos).value();
            float cold = Mth.clamp((0.15F - biome.getBaseTemperature()) / 0.15F, 0.0F, 1.0F);
            cold = CirrusEasing.smoothstep(cold);
            int weight = BIOME_SAMPLE_WEIGHTS[index];
            total += cold * weight;
            totalWeight += weight;
        }
        return total / totalWeight;
    }

    private void prepareDome() {
        if (domeBuffer == null) {
            domeBuffer = CirrusSkyDome.create(
                    DOME_RADIUS, AZIMUTH_SEGMENTS, ELEVATION_SEGMENTS, MIN_ELEVATION, MAX_ELEVATION
            );
        }
    }

    private float updateAnimationTime(ClientLevel level, float partialTick, int ticks) {
        double renderTime = ticks + (double)partialTick;
        double visualDayTime = CirrusTimeTransition.visualDayTime(level, partialTick);
        if (animationLevel != level
                || Double.isNaN(previousAnimationRenderTime)
                || Double.isNaN(previousAnimationVisualDayTime)) {
            animationLevel = level;
            animationTime = 0.0F;
            previousAnimationRenderTime = renderTime;
            previousAnimationVisualDayTime = visualDayTime;
            return animationTime;
        }

        double renderTimeDelta = Math.max(0.0, renderTime - previousAnimationRenderTime);
        double visualTimeDelta = Math.max(0.0, visualDayTime - previousAnimationVisualDayTime);
        previousAnimationRenderTime = renderTime;
        previousAnimationVisualDayTime = visualDayTime;

        // Preserve ordinary animation when daylight is frozen. Smooth time
        // transitions contribute only a twelfth of their extra acceleration so
        // the aurora reacts without racing through several shapes at once.
        double acceleratedExtra = Math.max(0.0, visualTimeDelta - renderTimeDelta);
        boolean timeTransitionActive = CirrusTimeTransition.isTransitionActive();
        double transitionScale = timeTransitionActive || previousTimeTransitionActive
                ? 1.0 / 12.0
                : 1.0;
        previousTimeTransitionActive = timeTransitionActive;
        double elapsedTicks = Math.min(renderTimeDelta + acceleratedExtra * transitionScale, 1200.0);
        animationTime += (float)(elapsedTicks
                * CirrusConfig.AURORA_ANIMATION_SPEED.get()
                / 20.0);
        return animationTime;
    }

    private void setNightlyVariation(CirrusUniform uniform, ClientLevel level) {
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
        long dimensionSalt = (long)level.dimension().identifier().hashCode() * 0x9E3779B97F4A7C15L;
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
        previousFrameTime = Double.NaN;
        sampledBiomeLevel = null;
        lastBiomeSampleTick = 0;
        animationLevel = null;
        animationTime = 0.0F;
        previousAnimationRenderTime = Double.NaN;
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
