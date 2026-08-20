package com.jvn.cirrus.client;

import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CirrusCloudAttachment {
    private static final ResourceLocation CLOUDS_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final double WORLD_SCALE = 12.0;
    private static final double PATTERN_SIZE = 256.0;
    private static final int SEARCH_RADIUS = 12;
    private static final int MINIMUM_ALPHA = 26;

    private static CloudPattern cloudPattern;
    private static boolean patternLoadAttempted;
    private static int cloudRenderTicks;
    private static final Map<LightningBolt, Vec3> ATTACHMENT_CACHE = new IdentityHashMap<>();

    private CirrusCloudAttachment() {
    }

    public static Vec3 findVisualOrigin(LightningBolt lightning, float partialTick) {
        Vec3 cached = ATTACHMENT_CACHE.get(lightning);
        if (cached != null) {
            return cached;
        }
        Vec3 attachment = findVisualOriginUncached(lightning, partialTick);
        ATTACHMENT_CACHE.put(lightning, attachment);
        return attachment;
    }

    private static Vec3 findVisualOriginUncached(LightningBolt lightning, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(lightning.level() instanceof ClientLevel level)
                || !CirrusCloudMode.isActive(minecraft.options.getCloudsType())) {
            return new Vec3(0.0, 128.0, 0.0);
        }

        float baseCloudHeight = CirrusRenderContext.cloudHeight(level);
        if (Float.isNaN(baseCloudHeight)) {
            return new Vec3(0.0, 128.0, 0.0);
        }

        List<Layer> layers = activeLayers(baseCloudHeight);
        CloudPattern pattern = cloudPattern();
        if (pattern != null) {
            double windSample = (cloudRenderTicks + partialTick) * 0.03 / WORLD_SCALE;
            for (Layer layer : layers) {
                double relativeHeight = layer.height() - lightning.getY() + 0.33;
                if (relativeHeight <= 4.0) {
                    continue;
                }
                Vec3 attachment = nearestOpaqueTexel(lightning, layer, pattern, windSample, relativeHeight);
                if (attachment != null) {
                    return attachment;
                }
            }
        }

        for (Layer layer : layers) {
            double relativeHeight = layer.height() - lightning.getY() + 0.33;
            if (relativeHeight > 4.0) {
                return new Vec3(0.0, relativeHeight, 0.0);
            }
        }
        return new Vec3(0.0, 128.0, 0.0);
    }

    private static List<Layer> activeLayers(float baseCloudHeight) {
        List<Layer> layers = new ArrayList<>(3);
        double lowerHeight = baseCloudHeight + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get();
        layers.add(new Layer(lowerHeight, CirrusConfig.LOWER_LAYER_SPEED.get(), 0, 0));

        double upperHeight = lowerHeight + CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get();
        if (CirrusConfig.UPPER_LAYER_ENABLED.get()) {
            layers.add(new Layer(upperHeight, CirrusConfig.UPPER_LAYER_SPEED.get(), 37, 91));
        }

        if (CirrusConfig.TOP_LAYER_ENABLED.get()) {
            double topHeight = upperHeight + CirrusConfig.TOP_LAYER_HEIGHT_OFFSET.get();
            layers.add(new Layer(topHeight, CirrusConfig.TOP_LAYER_SPEED.get(), 113, 173));
        }
        return layers;
    }

    private static Vec3 nearestOpaqueTexel(
            LightningBolt lightning,
            Layer layer,
            CloudPattern pattern,
            double windSample,
            double relativeHeight
    ) {
        double sampleX = lightning.getX() / WORLD_SCALE
                + windSample * layer.speed()
                + layer.patternOffsetX();
        double sampleZ = lightning.getZ() / WORLD_SCALE + layer.patternOffsetZ();
        int centerX = Mth.floor(sampleX);
        int centerZ = Mth.floor(sampleZ);
        Vec3 nearest = null;
        double nearestScore = Double.POSITIVE_INFINITY;

        for (int offsetZ = -SEARCH_RADIUS; offsetZ <= SEARCH_RADIUS; offsetZ++) {
            for (int offsetX = -SEARCH_RADIUS; offsetX <= SEARCH_RADIUS; offsetX++) {
                int sampleCellX = centerX + offsetX;
                int sampleCellZ = centerZ + offsetZ;
                int alpha = pattern.alphaAt(sampleCellX, sampleCellZ);
                if (alpha < MINIMUM_ALPHA) {
                    continue;
                }

                double worldOffsetX = (sampleCellX + 0.5 - sampleX) * WORLD_SCALE;
                double worldOffsetZ = (sampleCellZ + 0.5 - sampleZ) * WORLD_SCALE;
                double densityPenalty = (1.0 - alpha / 255.0) * WORLD_SCALE * WORLD_SCALE;
                double score = worldOffsetX * worldOffsetX + worldOffsetZ * worldOffsetZ + densityPenalty;
                if (score < nearestScore) {
                    nearestScore = score;
                    nearest = new Vec3(worldOffsetX, relativeHeight, worldOffsetZ);
                }
            }
        }
        return nearest;
    }

    private static CloudPattern cloudPattern() {
        if (patternLoadAttempted) {
            return cloudPattern;
        }
        patternLoadAttempted = true;
        Optional<Resource> resource = Minecraft.getInstance()
                .getResourceManager()
                .getResource(CLOUDS_LOCATION);
        if (resource.isEmpty()) {
            return null;
        }

        try (InputStream input = resource.get().open(); NativeImage image = NativeImage.read(input)) {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] alpha = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    alpha[y * width + x] = image.getPixel(x, y) >>> 24 & 0xFF;
                }
            }
            cloudPattern = new CloudPattern(width, height, alpha);
        } catch (IOException ignored) {
            cloudPattern = null;
        }
        return cloudPattern;
    }

    public static void updateRenderTicks(int ticks) {
        if (cloudRenderTicks != ticks) {
            ATTACHMENT_CACHE.clear();
        }
        cloudRenderTicks = ticks;
    }

    public static void invalidate() {
        cloudPattern = null;
        patternLoadAttempted = false;
        ATTACHMENT_CACHE.clear();
    }

    private record Layer(double height, double speed, int patternOffsetX, int patternOffsetZ) {
    }

    private record CloudPattern(int width, int height, int[] alpha) {
        private int alphaAt(int sampleX, int sampleZ) {
            double wrappedX = Mth.positiveModulo(sampleX, (int)PATTERN_SIZE) / PATTERN_SIZE;
            double wrappedZ = Mth.positiveModulo(sampleZ, (int)PATTERN_SIZE) / PATTERN_SIZE;
            int pixelX = Math.min(Mth.floor(wrappedX * width), width - 1);
            int pixelZ = Math.min(Mth.floor(wrappedZ * height), height - 1);
            return alpha[pixelZ * width + pixelX];
        }
    }
}
