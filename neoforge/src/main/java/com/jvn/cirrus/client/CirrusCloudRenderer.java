package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.toucanlib.client.ToucanEasing;
import com.jvn.toucanlib.neoforge.client.ToucanShaders;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CirrusCloudRenderer implements AutoCloseable {
    private static final float WORLD_SCALE = 12.0F;
    private static final int TILE_SIZE = 8;
    private static final float FANCY_CLOUD_THICKNESS = 4.0F;
    private static final float SURFACE_EPSILON = 9.765625E-4F;
    private static final float UV_SCALE = 1.0F / 256.0F;
    private static final float TWILIGHT_TRANSITION = (float)Math.toRadians(12.0);
    private static final ResourceLocation CLOUDS_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final int UPPER_PATTERN_OFFSET_X = 37;
    private static final int UPPER_PATTERN_OFFSET_Z = 91;
    private static final int TOP_PATTERN_OFFSET_X = 113;
    private static final int TOP_PATTERN_OFFSET_Z = 173;
    private static final LayerDefinition LOWER_LAYER = new LayerDefinition(LayerKind.LOWER, 0, 0);
    private static final LayerDefinition UPPER_LAYER = new LayerDefinition(
            LayerKind.UPPER,
            UPPER_PATTERN_OFFSET_X,
            UPPER_PATTERN_OFFSET_Z
    );
    private static final LayerDefinition TOP_LAYER = new LayerDefinition(
            LayerKind.TOP,
            TOP_PATTERN_OFFSET_X,
            TOP_PATTERN_OFFSET_Z
    );

    private final LayerMesh lowerMesh = new LayerMesh();
    private final LayerMesh upperMesh = new LayerMesh();
    private final LayerMesh topMesh = new LayerMesh();

    public void renderCelestialMask(
            ClientLevel level,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            int ticks,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        float cloudHeight = level.effects().getCloudHeight();
        if (Float.isNaN(cloudHeight)) {
            return;
        }

        int distanceChunks = CirrusConfig.CLOUD_RENDER_DISTANCE.get();
        double cameraSampleX = cameraX / WORLD_SCALE;
        double cameraSampleZ = cameraZ / WORLD_SCALE;
        double windSample = (ticks + partialTick) * 0.03 / WORLD_SCALE;
        float rainLevel = smoothWeatherLevel(level.getRainLevel(partialTick));
        float thunderLevel = smoothWeatherLevel(level.getThunderLevel(partialTick));
        prepareLayer(lowerMesh, LOWER_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample);
        boolean upperEnabled = CirrusConfig.UPPER_LAYER_ENABLED.get();
        boolean topEnabled = CirrusConfig.TOP_LAYER_ENABLED.get();
        if (upperEnabled) {
            prepareLayer(upperMesh, UPPER_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample);
        }
        if (topEnabled) {
            prepareLayer(topMesh, TOP_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample);
        }

        int oldTexture = RenderSystem.getShaderTexture(0);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);
        try {
            drawMaskLayer(lowerMesh, LOWER_LAYER, frustumMatrix, projectionMatrix,
                    cloudHeight + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get() - cameraY + 0.33,
                    cameraSampleX, cameraSampleZ, windSample, rainLevel, thunderLevel);
            if (upperEnabled) {
                drawMaskLayer(upperMesh, UPPER_LAYER, frustumMatrix, projectionMatrix,
                        cloudHeight + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get()
                                + CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get() - cameraY + 0.33,
                        cameraSampleX, cameraSampleZ, windSample, rainLevel, thunderLevel);
            }
            if (topEnabled) {
                drawMaskLayer(topMesh, TOP_LAYER, frustumMatrix, projectionMatrix,
                        cloudHeight + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get()
                                + CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get()
                                + CirrusConfig.TOP_LAYER_HEIGHT_OFFSET.get() - cameraY + 0.33,
                        cameraSampleX, cameraSampleZ, windSample, rainLevel, thunderLevel);
            }
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderTexture(0, oldTexture);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
        }
    }

    private static void drawMaskLayer(
            LayerMesh mesh,
            LayerDefinition definition,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            double relativeHeight,
            double cameraSampleX,
            double cameraSampleZ,
            double windSample,
            float rainLevel,
            float thunderLevel
    ) {
        if (mesh.buffer == null) {
            return;
        }
        double sampleX = sampleX(definition, cameraSampleX, windSample);
        double sampleZ = sampleZ(definition, cameraSampleZ);
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        poseStack.scale(WORLD_SCALE, 1.0F, WORLD_SCALE);
        poseStack.translate(-(sampleX - mesh.cachedAnchorX), relativeHeight, -(sampleZ - mesh.cachedAnchorZ));
        mesh.buffer.bind();
        ShaderInstance shader = CirrusShaders.cloudMask();
        ToucanShaders.setUniform(
                shader,
                "CirrusRainCloudCoverage",
                rainLevel * CirrusConfig.RAIN_CLOUD_COVERAGE.get().floatValue()
        );
        ToucanShaders.setUniform(
                shader,
                "CirrusThunderCloudCoverage",
                thunderLevel * CirrusConfig.THUNDER_CLOUD_COVERAGE.get().floatValue()
        );
        ToucanShaders.setUniform(shader, "CirrusLayerOpacity", definition.opacity(rainLevel, thunderLevel));
        mesh.buffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
    }

    public void render(
            ClientLevel level,
            PoseStack poseStack,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            int ticks,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        float cloudHeight = level.effects().getCloudHeight();
        if (Float.isNaN(cloudHeight)) {
            return;
        }

        int distanceChunks = CirrusConfig.CLOUD_RENDER_DISTANCE.get();
        double cameraSampleX = cameraX / WORLD_SCALE;
        double cameraSampleZ = cameraZ / WORLD_SCALE;
        double cloudTime = ticks + partialTick;
        double windSample = cloudTime * 0.03 / WORLD_SCALE;
        float celestialAngle = level.getSunAngle(partialTick);
        float sunWeight = celestialSunWeight(celestialAngle);
        Vector3f celestialViewDirection = celestialViewDirection(frustumMatrix, celestialAngle);
        float rainLevel = smoothWeatherLevel(level.getRainLevel(partialTick));
        float thunderLevel = smoothWeatherLevel(level.getThunderLevel(partialTick));
        LightningState lightning = lightningState(
                level,
                frustumMatrix,
                partialTick,
                ticks,
                cameraX,
                cameraY,
                cameraZ,
                distanceChunks * 16.0F
        );

        prepareLayer(lowerMesh, LOWER_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample);
        boolean upperEnabled = CirrusConfig.UPPER_LAYER_ENABLED.get();
        if (upperEnabled) {
            prepareLayer(upperMesh, UPPER_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample);
        } else {
            upperMesh.invalidate();
        }
        boolean topEnabled = CirrusConfig.TOP_LAYER_ENABLED.get();
        if (topEnabled) {
            prepareLayer(topMesh, TOP_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample);
        } else {
            topMesh.invalidate();
        }
        if (lowerMesh.buffer == null && upperMesh.buffer == null && topMesh.buffer == null) {
            return;
        }

        float oldFogStart = RenderSystem.getShaderFogStart();
        float oldFogEnd = RenderSystem.getShaderFogEnd();
        FogShape oldFogShape = RenderSystem.getShaderFogShape();
        float[] oldFogColor = RenderSystem.getShaderFogColor();
        float oldFogRed = oldFogColor[0];
        float oldFogGreen = oldFogColor[1];
        float oldFogBlue = oldFogColor[2];
        float[] oldShaderColor = RenderSystem.getShaderColor();
        float oldShaderRed = oldShaderColor[0];
        float oldShaderGreen = oldShaderColor[1];
        float oldShaderBlue = oldShaderColor[2];
        float oldShaderAlpha = oldShaderColor[3];
        float distanceBlocks = distanceChunks * 16.0F;
        float fadeLength = Math.max(32.0F, distanceBlocks * 0.15F);
        Vec3 cloudColor = level.getCloudColor(partialTick);
        boolean preserveVanillaFog = hasVisibilityLimitingFog(level, cameraX, cameraZ);

        try {
            FogRenderer.levelFogColor();
            if (!preserveVanillaFog) {
                RenderSystem.setShaderFogStart(Math.max(0.0F, distanceBlocks - fadeLength));
                RenderSystem.setShaderFogEnd(distanceBlocks);
                RenderSystem.setShaderFogShape(FogShape.CYLINDER);
            }
            RenderSystem.setShaderColor((float)cloudColor.x, (float)cloudColor.y, (float)cloudColor.z, 1.0F);

            drawLayer(
                    lowerMesh,
                    LOWER_LAYER,
                    poseStack,
                    frustumMatrix,
                    projectionMatrix,
                    cloudHeight + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get() - cameraY + 0.33,
                    cameraSampleX,
                    cameraSampleZ,
                    windSample,
                    cloudColor,
                    celestialAngle,
                    sunWeight,
                    celestialViewDirection,
                    rainLevel,
                    thunderLevel,
                    lightning
            );
            if (upperEnabled) {
                double upperHeight = cloudHeight
                        + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get()
                        + CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get()
                        - cameraY
                        + 0.33;
                drawLayer(
                        upperMesh,
                        UPPER_LAYER,
                        poseStack,
                        frustumMatrix,
                        projectionMatrix,
                        upperHeight,
                        cameraSampleX,
                        cameraSampleZ,
                        windSample,
                        cloudColor,
                        celestialAngle,
                        sunWeight,
                        celestialViewDirection,
                        rainLevel,
                        thunderLevel,
                        lightning
                );
            }
            if (topEnabled) {
                double topHeight = cloudHeight
                        + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get()
                        + CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get()
                        + CirrusConfig.TOP_LAYER_HEIGHT_OFFSET.get()
                        - cameraY
                        + 0.33;
                drawLayer(
                        topMesh,
                        TOP_LAYER,
                        poseStack,
                        frustumMatrix,
                        projectionMatrix,
                        topHeight,
                        cameraSampleX,
                        cameraSampleZ,
                        windSample,
                        cloudColor,
                        celestialAngle,
                        sunWeight,
                        celestialViewDirection,
                        rainLevel,
                        thunderLevel,
                        lightning
                );
            }
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(oldShaderRed, oldShaderGreen, oldShaderBlue, oldShaderAlpha);
            RenderSystem.setShaderFogColor(oldFogRed, oldFogGreen, oldFogBlue);
            RenderSystem.setShaderFogStart(oldFogStart);
            RenderSystem.setShaderFogEnd(oldFogEnd);
            RenderSystem.setShaderFogShape(oldFogShape);
        }
    }

    private static boolean hasVisibilityLimitingFog(ClientLevel level, double cameraX, double cameraZ) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity cameraEntity = minecraft.getCameraEntity();
        if (cameraEntity instanceof LivingEntity livingEntity
                && (livingEntity.hasEffect(MobEffects.BLINDNESS)
                        || livingEntity.hasEffect(MobEffects.DARKNESS))) {
            return true;
        }
        if (minecraft.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return true;
        }
        return level.effects().isFoggyAt(Mth.floor(cameraX), Mth.floor(cameraZ))
                || minecraft.gui.getBossOverlay().shouldCreateWorldFog();
    }

    private void prepareLayer(
            LayerMesh mesh,
            LayerDefinition definition,
            ClientLevel level,
            int distanceChunks,
            double cameraSampleX,
            double cameraSampleZ,
            double windSample
    ) {
        CirrusConfig.CloudStyle style = definition.style();
        double sampleX = sampleX(definition, cameraSampleX, windSample);
        double sampleZ = sampleZ(definition, cameraSampleZ);
        int anchorX = Mth.floor(sampleX / TILE_SIZE) * TILE_SIZE;
        int anchorZ = Mth.floor(sampleZ / TILE_SIZE) * TILE_SIZE;
        if (mesh.buffer != null
                && mesh.cachedLevel == level
                && mesh.cachedDistanceChunks == distanceChunks
                && mesh.cachedStyle == style
                && mesh.cachedAnchorX == anchorX
                && mesh.cachedAnchorZ == anchorZ) {
            return;
        }

        mesh.closeBuffer();
        MeshData builtMesh = buildMesh(distanceChunks, anchorX, anchorZ, style);
        mesh.buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        mesh.buffer.bind();
        mesh.buffer.upload(builtMesh);
        VertexBuffer.unbind();
        mesh.cachedLevel = level;
        mesh.cachedDistanceChunks = distanceChunks;
        mesh.cachedStyle = style;
        mesh.cachedAnchorX = anchorX;
        mesh.cachedAnchorZ = anchorZ;
    }

    private void drawLayer(
            LayerMesh mesh,
            LayerDefinition definition,
            PoseStack poseStack,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            double relativeHeight,
            double cameraSampleX,
            double cameraSampleZ,
            double windSample,
            Vec3 cloudColor,
            float celestialAngle,
            float sunWeight,
            Vector3f celestialViewDirection,
            float rainLevel,
            float thunderLevel,
            LightningState lightning
    ) {
        if (mesh.buffer == null) {
            return;
        }
        double sampleX = sampleX(definition, cameraSampleX, windSample);
        double sampleZ = sampleZ(definition, cameraSampleZ);
        poseStack.pushPose();
        try {
            RenderSystem.setShaderColor(
                    (float)cloudColor.x,
                    (float)cloudColor.y,
                    (float)cloudColor.z,
                    definition.opacity(rainLevel, thunderLevel)
            );
            poseStack.mulPose(frustumMatrix);
            poseStack.scale(WORLD_SCALE, 1.0F, WORLD_SCALE);
            poseStack.translate(
                    -(sampleX - mesh.cachedAnchorX),
                    relativeHeight,
                    -(sampleZ - mesh.cachedAnchorZ)
            );

            mesh.buffer.bind();
            if (definition.style() == CirrusConfig.CloudStyle.FANCY) {
                RenderType depthOnly = RenderType.cloudsDepthOnly();
                depthOnly.setupRenderState();
                try {
                    ShaderInstance shader = CirrusShaders.clouds();
                    setCloudEnvironment(
                            shader,
                            celestialAngle,
                            sunWeight,
                            celestialViewDirection,
                            rainLevel,
                            thunderLevel,
                            lightning
                    );
                    mesh.buffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
                } finally {
                    depthOnly.clearRenderState();
                }
            }
            RenderType clouds = RenderType.clouds();
            clouds.setupRenderState();
            try {
                ShaderInstance shader = CirrusShaders.clouds();
                setCloudEnvironment(
                        shader,
                        celestialAngle,
                        sunWeight,
                        celestialViewDirection,
                        rainLevel,
                        thunderLevel,
                        lightning
                );
                mesh.buffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            } finally {
                clouds.clearRenderState();
            }
            if (Minecraft.useShaderTransparency()) {
                RenderType depthOnly = RenderType.cloudsDepthOnly();
                depthOnly.setupRenderState();
                try {
                    Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
                    ShaderInstance shader = CirrusShaders.clouds();
                    setCloudEnvironment(
                            shader,
                            celestialAngle,
                            sunWeight,
                            celestialViewDirection,
                            rainLevel,
                            thunderLevel,
                            lightning
                    );
                    mesh.buffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
                } finally {
                    depthOnly.clearRenderState();
                }
            }
            VertexBuffer.unbind();
        } finally {
            VertexBuffer.unbind();
            poseStack.popPose();
        }
    }

    private static double wrapSample(double sample) {
        return sample - Mth.floor(sample / 2048.0) * 2048.0;
    }

    private static double sampleX(LayerDefinition definition, double cameraSampleX, double windSample) {
        return wrapSample(cameraSampleX + windSample * definition.speed() + definition.patternOffsetX);
    }

    private static double sampleZ(LayerDefinition definition, double cameraSampleZ) {
        return wrapSample(cameraSampleZ + definition.patternOffsetZ);
    }

    private static Vector3f celestialViewDirection(Matrix4f frustumMatrix, float celestialAngle) {
        Vector3f direction = new Vector3f(
                -Mth.sin(celestialAngle),
                Mth.cos(celestialAngle),
                0.0F
        );
        return frustumMatrix.transformDirection(direction).normalize();
    }

    private static float celestialSunWeight(float celestialAngle) {
        float transitionHeight = Mth.sin(TWILIGHT_TRANSITION);
        float blend = Mth.clamp(
                (Mth.cos(celestialAngle) + transitionHeight) / (2.0F * transitionHeight),
                0.0F,
                1.0F
        );
        return ToucanEasing.smoothstep(blend);
    }

    private static float smoothWeatherLevel(float level) {
        return ToucanEasing.smoothstep(level);
    }

    private LightningState lightningState(
            ClientLevel level,
            Matrix4f frustumMatrix,
            float partialTick,
            int ticks,
            double cameraX,
            double cameraY,
            double cameraZ,
            float cloudRenderDistance
    ) {
        float intensity = CirrusConfig.HIDE_LIGHTNING_CLOUD_FLASHES.get()
                || Minecraft.getInstance().options.hideLightningFlash().get()
                ? 0.0F
                : Mth.clamp(level.getSkyFlashTime() - partialTick, 0.0F, 1.0F)
                        * CirrusConfig.LIGHTNING_CLOUD_FLASH_OPACITY.get().floatValue();
        Vector3f viewUp = frustumMatrix.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
        float radius = Mth.clamp(cloudRenderDistance * 0.70F, 320.0F, 768.0F);
        if (intensity <= 0.0F) {
            return new LightningState(0.0F, new Vector3f(), viewUp, radius);
        }

        LightningBolt nearestLightning = CirrusLightningLocator.nearestHorizontal(
                level, cameraX, cameraZ
        );
        if (nearestLightning == null) {
            return new LightningState(0.0F, new Vector3f(), viewUp, radius);
        }

        Vec3 visualOrigin = CirrusCloudAttachment.findVisualOrigin(nearestLightning, partialTick);
        Vector3f viewPosition = new Vector3f(
                (float)(nearestLightning.getX() + visualOrigin.x - cameraX),
                (float)(nearestLightning.getY() + visualOrigin.y - cameraY),
                (float)(nearestLightning.getZ() + visualOrigin.z - cameraZ)
        );
        frustumMatrix.transformPosition(viewPosition);
        return new LightningState(intensity, viewPosition, viewUp, radius);
    }

    private static void setCloudEnvironment(
            ShaderInstance shader,
            float celestialAngle,
            float sunWeight,
            Vector3f celestialViewDirection,
            float rainLevel,
            float thunderLevel,
            LightningState lightning
    ) {
        ToucanShaders.setUniform(shader, "CirrusEnabled", true);
        Uniform lightDirection = shader.getUniform("CirrusLightDirection");
        if (lightDirection == null) {
            return;
        }
        lightDirection.set(-Mth.sin(celestialAngle), 0.0F);
        ToucanShaders.setUniform(shader, "CirrusLightColor", 1.0F, 0.82F, 0.55F);
        ToucanShaders.setUniform(shader, "CirrusSunWeight", sunWeight);
        ToucanShaders.setUniform(
                shader, "CirrusLightViewDirection",
                celestialViewDirection.x, celestialViewDirection.y, celestialViewDirection.z
        );
        ToucanShaders.setUniform(shader, "CirrusRainLevel", rainLevel);
        ToucanShaders.setUniform(shader, "CirrusThunderLevel", thunderLevel);
        ToucanShaders.setUniform(
                shader,
                "CirrusRainCloudCoverage",
                rainLevel * CirrusConfig.RAIN_CLOUD_COVERAGE.get().floatValue()
        );
        ToucanShaders.setUniform(
                shader,
                "CirrusThunderCloudCoverage",
                thunderLevel * CirrusConfig.THUNDER_CLOUD_COVERAGE.get().floatValue()
        );
        ToucanShaders.setUniform(shader, "CirrusLightningFlash", lightning.intensity());
        ToucanShaders.setUniform(
                shader, "CirrusLightningViewPosition",
                lightning.viewPosition().x, lightning.viewPosition().y, lightning.viewPosition().z
        );
        ToucanShaders.setUniform(
                shader, "CirrusWorldUpViewDirection",
                lightning.viewUp().x, lightning.viewUp().y, lightning.viewUp().z
        );
        ToucanShaders.setUniform(shader, "CirrusLightningRadius", lightning.radius());
    }

    private MeshData buildMesh(
            int distanceChunks, int anchorX, int anchorZ, CirrusConfig.CloudStyle style
    ) {
        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );
        float radius = distanceChunks * 16.0F / WORLD_SCALE;
        int tileRadius = Mth.ceil(radius / TILE_SIZE) + 1;
        float inclusionRadius = radius + TILE_SIZE * 0.7072F;
        float inclusionRadiusSquared = inclusionRadius * inclusionRadius;

        for (int tileX = -tileRadius; tileX <= tileRadius; tileX++) {
            for (int tileZ = -tileRadius; tileZ <= tileRadius; tileZ++) {
                float centerX = (tileX + 0.5F) * TILE_SIZE;
                float centerZ = (tileZ + 0.5F) * TILE_SIZE;
                if (centerX * centerX + centerZ * centerZ > inclusionRadiusSquared) {
                    continue;
                }
                float x = tileX * TILE_SIZE;
                float z = tileZ * TILE_SIZE;
                if (style == CirrusConfig.CloudStyle.FANCY) {
                    addFancyTile(builder, tileX, tileZ, x, z, anchorX, anchorZ);
                } else {
                    addFastTile(builder, x, z, anchorX, anchorZ);
                }
            }
        }
        return builder.buildOrThrow();
    }

    private static void addFastTile(
            BufferBuilder builder,
            float x,
            float z,
            int anchorX,
            int anchorZ
    ) {
        float x1 = x + TILE_SIZE;
        float z1 = z + TILE_SIZE;
        vertex(builder, x, 0.0F, z, x, z, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
        vertex(builder, x, 0.0F, z1, x, z1, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
        vertex(builder, x1, 0.0F, z1, x1, z1, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
        vertex(builder, x1, 0.0F, z, x1, z, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
    }

    private static void addFancyTile(
            BufferBuilder builder,
            int tileX,
            int tileZ,
            float x,
            float z,
            int anchorX,
            int anchorZ
    ) {
        float x1 = x + TILE_SIZE;
        float z1 = z + TILE_SIZE;
        float top = FANCY_CLOUD_THICKNESS - SURFACE_EPSILON;

        vertex(builder, x, 0.0F, z1, x, z1, anchorX, anchorZ, 0.70F, 0.0F, -1.0F, 0.0F);
        vertex(builder, x1, 0.0F, z1, x1, z1, anchorX, anchorZ, 0.70F, 0.0F, -1.0F, 0.0F);
        vertex(builder, x1, 0.0F, z, x1, z, anchorX, anchorZ, 0.70F, 0.0F, -1.0F, 0.0F);
        vertex(builder, x, 0.0F, z, x, z, anchorX, anchorZ, 0.70F, 0.0F, -1.0F, 0.0F);

        vertex(builder, x, top, z1, x, z1, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
        vertex(builder, x1, top, z1, x1, z1, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
        vertex(builder, x1, top, z, x1, z, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
        vertex(builder, x, top, z, x, z, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);

        if (tileX >= 0) {
            for (int cell = 0; cell < TILE_SIZE; cell++) {
                float sideX = x + cell;
                float sampleX = x + cell + 0.5F;
                vertex(
                        builder, sideX, 0.0F, z1, sampleX, z1, anchorX, anchorZ,
                        0.90F, -1.0F, 0.0F, 0.0F
                );
                vertex(
                        builder, sideX, FANCY_CLOUD_THICKNESS, z1, sampleX, z1, anchorX, anchorZ,
                        0.90F, -1.0F, 0.0F, 0.0F
                );
                vertex(
                        builder, sideX, FANCY_CLOUD_THICKNESS, z, sampleX, z, anchorX, anchorZ,
                        0.90F, -1.0F, 0.0F, 0.0F
                );
                vertex(
                        builder, sideX, 0.0F, z, sampleX, z, anchorX, anchorZ,
                        0.90F, -1.0F, 0.0F, 0.0F
                );
            }
        }
        if (tileX <= 1) {
            for (int cell = 0; cell < TILE_SIZE; cell++) {
                float sideX = x + cell + 1.0F - SURFACE_EPSILON;
                float sampleX = x + cell + 0.5F;
                vertex(
                        builder, sideX, 0.0F, z1, sampleX, z1, anchorX, anchorZ,
                        0.90F, 1.0F, 0.0F, 0.0F
                );
                vertex(
                        builder, sideX, FANCY_CLOUD_THICKNESS, z1, sampleX, z1, anchorX, anchorZ,
                        0.90F, 1.0F, 0.0F, 0.0F
                );
                vertex(
                        builder, sideX, FANCY_CLOUD_THICKNESS, z, sampleX, z, anchorX, anchorZ,
                        0.90F, 1.0F, 0.0F, 0.0F
                );
                vertex(
                        builder, sideX, 0.0F, z, sampleX, z, anchorX, anchorZ,
                        0.90F, 1.0F, 0.0F, 0.0F
                );
            }
        }
        if (tileZ >= 0) {
            for (int cell = 0; cell < TILE_SIZE; cell++) {
                float sideZ = z + cell;
                float sampleZ = z + cell + 0.5F;
                vertex(
                        builder, x, FANCY_CLOUD_THICKNESS, sideZ, x, sampleZ, anchorX, anchorZ,
                        0.80F, 0.0F, 0.0F, -1.0F
                );
                vertex(
                        builder, x1, FANCY_CLOUD_THICKNESS, sideZ, x1, sampleZ, anchorX, anchorZ,
                        0.80F, 0.0F, 0.0F, -1.0F
                );
                vertex(
                        builder, x1, 0.0F, sideZ, x1, sampleZ, anchorX, anchorZ,
                        0.80F, 0.0F, 0.0F, -1.0F
                );
                vertex(
                        builder, x, 0.0F, sideZ, x, sampleZ, anchorX, anchorZ,
                        0.80F, 0.0F, 0.0F, -1.0F
                );
            }
        }
        if (tileZ <= 1) {
            for (int cell = 0; cell < TILE_SIZE; cell++) {
                float sideZ = z + cell + 1.0F - SURFACE_EPSILON;
                float sampleZ = z + cell + 0.5F;
                vertex(
                        builder, x, FANCY_CLOUD_THICKNESS, sideZ, x, sampleZ, anchorX, anchorZ,
                        0.80F, 0.0F, 0.0F, 1.0F
                );
                vertex(
                        builder, x1, FANCY_CLOUD_THICKNESS, sideZ, x1, sampleZ, anchorX, anchorZ,
                        0.80F, 0.0F, 0.0F, 1.0F
                );
                vertex(
                        builder, x1, 0.0F, sideZ, x1, sampleZ, anchorX, anchorZ,
                        0.80F, 0.0F, 0.0F, 1.0F
                );
                vertex(
                        builder, x, 0.0F, sideZ, x, sampleZ, anchorX, anchorZ,
                        0.80F, 0.0F, 0.0F, 1.0F
                );
            }
        }
    }

    private static void vertex(
            BufferBuilder builder,
            float x,
            float y,
            float z,
            float sampleX,
            float sampleZ,
            int anchorX,
            int anchorZ,
            float shade,
            float normalX,
            float normalY,
            float normalZ
    ) {
        builder.addVertex(x, y, z)
                .setUv((sampleX + anchorX) * UV_SCALE, (sampleZ + anchorZ) * UV_SCALE)
                .setColor(shade, shade, shade, 1.0F)
                .setNormal(normalX, normalY, normalZ);
    }

    public void invalidate() {
        lowerMesh.invalidate();
        upperMesh.invalidate();
        topMesh.invalidate();
    }

    @Override
    public void close() {
        invalidate();
    }

    private record LayerDefinition(LayerKind kind, int patternOffsetX, int patternOffsetZ) {
        private double speed() {
            return switch (kind) {
                case LOWER -> CirrusConfig.LOWER_LAYER_SPEED.get();
                case UPPER -> CirrusConfig.UPPER_LAYER_SPEED.get();
                case TOP -> CirrusConfig.TOP_LAYER_SPEED.get();
            };
        }

        private CirrusConfig.CloudStyle style() {
            return switch (kind) {
                case LOWER -> CirrusConfig.LOWER_LAYER_STYLE.get();
                case UPPER -> CirrusConfig.UPPER_LAYER_STYLE.get();
                case TOP -> CirrusConfig.TOP_LAYER_STYLE.get();
            };
        }

        private float opacity(float rainLevel, float thunderLevel) {
            float baseOpacity = switch (kind) {
                case LOWER -> CirrusConfig.LOWER_LAYER_OPACITY.get().floatValue();
                case UPPER -> CirrusConfig.UPPER_LAYER_OPACITY.get().floatValue();
                case TOP -> CirrusConfig.TOP_LAYER_OPACITY.get().floatValue();
            };
            float rainBoost = switch (kind) {
                case LOWER -> 0.22F;
                case UPPER -> 0.16F;
                case TOP -> 0.12F;
            };
            float thunderBoost = switch (kind) {
                case LOWER -> 0.10F;
                case UPPER -> 0.08F;
                case TOP -> 0.06F;
            };
            return Mth.clamp(baseOpacity + rainLevel * rainBoost + thunderLevel * thunderBoost, 0.0F, 1.0F);
        }
    }

    private enum LayerKind {
        LOWER,
        UPPER,
        TOP
    }

    private record LightningState(float intensity, Vector3f viewPosition, Vector3f viewUp, float radius) {
    }

    private static final class LayerMesh {
        private VertexBuffer buffer;
        private ClientLevel cachedLevel;
        private int cachedDistanceChunks = -1;
        private CirrusConfig.CloudStyle cachedStyle;
        private int cachedAnchorX = Integer.MIN_VALUE;
        private int cachedAnchorZ = Integer.MIN_VALUE;

        private void invalidate() {
            closeBuffer();
            cachedLevel = null;
            cachedDistanceChunks = -1;
            cachedStyle = null;
            cachedAnchorX = Integer.MIN_VALUE;
            cachedAnchorZ = Integer.MIN_VALUE;
        }

        private void closeBuffer() {
            if (buffer != null) {
                buffer.close();
                buffer = null;
            }
        }
    }
}
