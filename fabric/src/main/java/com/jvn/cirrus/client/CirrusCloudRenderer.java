package com.jvn.cirrus.client;

import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import com.jvn.cirrus.client.compat.shaderpacks.CirrusShaderPackCompat;
import com.jvn.cirrus.config.CirrusConfig;
import com.jvn.cirrus.client.util.CirrusEasing;
import com.jvn.cirrus.client.util.CirrusShaderUniforms;
import com.jvn.cirrus.client.render.CirrusUniform;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.jvn.cirrus.client.render.CirrusVertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.jvn.cirrus.client.render.CirrusShader;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CirrusCloudRenderer implements AutoCloseable {
    private static final float WORLD_SCALE = 12.0F;
    private static final int TILE_SIZE = 8;
    private static final int DISTANT_RING_SEGMENTS = 256;
    private static final float FANCY_CLOUD_THICKNESS = 4.0F;
    private static final float SURFACE_EPSILON = 9.765625E-4F;
    private static final float UV_SCALE = 1.0F / 256.0F;
    private static final float TWILIGHT_TRANSITION = (float)Math.toRadians(12.0);
    private static final Identifier CLOUDS_LOCATION =
            Identifier.withDefaultNamespace("textures/environment/clouds.png");
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

    public void renderSunMask(
            ClientLevel level,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            int ticks,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        float cloudHeight = CirrusRenderContext.cloudHeight(level);
        if (Float.isNaN(cloudHeight)) {
            return;
        }

        int distanceChunks = DistantHorizonsCompat.cloudRenderDistanceChunks();
        double cameraSampleX = cameraX / WORLD_SCALE;
        double cameraSampleZ = cameraZ / WORLD_SCALE;
        double windSample = (ticks + partialTick) * 0.03 / WORLD_SCALE;
        float rainLevel = smoothWeatherLevel(level.getRainLevel(partialTick));
        float thunderLevel = smoothWeatherLevel(level.getThunderLevel(partialTick));
        prepareLayer(lowerMesh, LOWER_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample, false);
        boolean upperEnabled = CirrusConfig.UPPER_LAYER_ENABLED.get();
        boolean topEnabled = CirrusConfig.TOP_LAYER_ENABLED.get();
        if (upperEnabled) {
            prepareLayer(upperMesh, UPPER_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample, false);
        }
        if (topEnabled) {
            prepareLayer(topMesh, TOP_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample, false);
        }

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
        CirrusShader shader = CirrusShaders.cloudMask();
        CirrusShaderUniforms.setUniform(
                shader,
                "CirrusRainCloudCoverage",
                rainLevel * CirrusConfig.RAIN_CLOUD_COVERAGE.get().floatValue()
        );
        CirrusShaderUniforms.setUniform(
                shader,
                "CirrusThunderCloudCoverage",
                thunderLevel * CirrusConfig.THUNDER_CLOUD_COVERAGE.get().floatValue()
        );
        CirrusShaderUniforms.setUniform(shader, "CirrusLayerOpacity", definition.opacity(rainLevel, thunderLevel));
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
        float cloudHeight = CirrusRenderContext.cloudHeight(level);
        if (Float.isNaN(cloudHeight)) {
            return;
        }

        int distanceChunks = DistantHorizonsCompat.cloudRenderDistanceChunks();
        double cameraSampleX = cameraX / WORLD_SCALE;
        double cameraSampleZ = cameraZ / WORLD_SCALE;
        double cloudTime = ticks + partialTick;
        double windSample = cloudTime * 0.03 / WORLD_SCALE;
        boolean shaderPackInUse = CirrusShaderPackCompat.isShaderPackInUse();
        float celestialAngle = CirrusRenderContext.sunAngle(partialTick);
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

        prepareLayer(lowerMesh, LOWER_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample, shaderPackInUse);
        boolean upperEnabled = CirrusConfig.UPPER_LAYER_ENABLED.get();
        if (upperEnabled) {
            prepareLayer(upperMesh, UPPER_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample, shaderPackInUse);
        } else {
            upperMesh.invalidate();
        }
        boolean topEnabled = CirrusConfig.TOP_LAYER_ENABLED.get();
        if (topEnabled) {
            prepareLayer(topMesh, TOP_LAYER, level, distanceChunks, cameraSampleX, cameraSampleZ, windSample, shaderPackInUse);
        } else {
            topMesh.invalidate();
        }
        if (lowerMesh.buffer == null && upperMesh.buffer == null && topMesh.buffer == null) {
            return;
        }

        float distanceBlocks = distanceChunks * 16.0F;
        float fadeLength = Math.max(32.0F, distanceBlocks * 0.15F);
        Vec3 cloudColor = CirrusRenderContext.cloudColor(partialTick);
        double lowerHeight = cloudHeight
                + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get()
                - cameraY
                + 0.33;
        double upperHeight = lowerHeight + CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get();
        double topHeight = upperHeight + CirrusConfig.TOP_LAYER_HEIGHT_OFFSET.get();
        boolean translucentLayerOverlap = CirrusConfig.TRANSLUCENT_LAYER_OVERLAP.get();
        double lowerDistance = translucentLayerOverlap ? Math.abs(lowerHeight) : 3.0;
        double upperDistance = upperEnabled
                ? (translucentLayerOverlap ? Math.abs(upperHeight) : 2.0)
                : -1.0;
        double topDistance = topEnabled
                ? (translucentLayerOverlap ? Math.abs(topHeight) : 1.0)
                : -1.0;
        int enabledLayerCount = 1 + (upperEnabled ? 1 : 0) + (topEnabled ? 1 : 0);
        CirrusShader cloudShader = CirrusShaders.clouds();
        CirrusShaderUniforms.setUniform(
                cloudShader, "FogStart", Math.max(0.0F, distanceBlocks - fadeLength)
        );
        CirrusShaderUniforms.setUniform(cloudShader, "FogEnd", distanceBlocks);
        if (CirrusRenderContext.fogColor() != null) {
            CirrusShaderUniforms.setUniform(
                    cloudShader,
                    "FogColor",
                    CirrusRenderContext.fogColor().x,
                    CirrusRenderContext.fogColor().y,
                    CirrusRenderContext.fogColor().z,
                    CirrusRenderContext.fogColor().w
            );
        }

        for (int layerIndex = 0; layerIndex < enabledLayerCount; layerIndex++) {
            LayerMesh mesh;
            LayerDefinition definition;
            double relativeHeight;
            if (lowerDistance >= upperDistance && lowerDistance >= topDistance) {
                mesh = lowerMesh;
                definition = LOWER_LAYER;
                relativeHeight = lowerHeight;
                lowerDistance = -1.0;
            } else if (upperDistance >= topDistance) {
                mesh = upperMesh;
                definition = UPPER_LAYER;
                relativeHeight = upperHeight;
                upperDistance = -1.0;
            } else {
                mesh = topMesh;
                definition = TOP_LAYER;
                relativeHeight = topHeight;
                topDistance = -1.0;
            }
            drawLayer(
                    mesh,
                    definition,
                    poseStack,
                    frustumMatrix,
                    projectionMatrix,
                    relativeHeight,
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
    }


    private void prepareLayer(
            LayerMesh mesh,
            LayerDefinition definition,
            ClientLevel level,
            int distanceChunks,
            double cameraSampleX,
            double cameraSampleZ,
            double windSample,
            boolean shaderPackDistanceFade
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
                && mesh.cachedShaderPackDistanceFade == shaderPackDistanceFade
                && mesh.cachedAnchorX == anchorX
                && mesh.cachedAnchorZ == anchorZ) {
            return;
        }

        mesh.closeBuffer();
        ByteBufferBuilder sourceBuffer = new ByteBufferBuilder(1024);
        MeshData builtMesh = buildMesh(
                sourceBuffer, distanceChunks, anchorX, anchorZ, style, shaderPackDistanceFade
        );
        mesh.buffer = new CirrusVertexBuffer();
        mesh.buffer.upload(builtMesh, sourceBuffer);
        mesh.cachedLevel = level;
        mesh.cachedDistanceChunks = distanceChunks;
        mesh.cachedStyle = style;
        mesh.cachedShaderPackDistanceFade = shaderPackDistanceFade;
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
            poseStack.mulPose(frustumMatrix);
            poseStack.scale(WORLD_SCALE, 1.0F, WORLD_SCALE);
            poseStack.translate(
                    -(sampleX - mesh.cachedAnchorX),
                    relativeHeight,
                    -(sampleZ - mesh.cachedAnchorZ)
            );

            CirrusShader shader = CirrusShaders.clouds();
            CirrusShaderUniforms.setUniform(
                    shader,
                    "ColorModulator",
                    (float)cloudColor.x,
                    (float)cloudColor.y,
                    (float)cloudColor.z,
                    definition.opacity(rainLevel, thunderLevel)
            );
            setCloudEnvironment(
                    shader,
                    celestialAngle,
                    sunWeight,
                    celestialViewDirection,
                    rainLevel,
                    thunderLevel,
                    lightning
            );
            boolean renderingForDistantHorizons =
                    CirrusRenderContext.renderingCloudsForDistantHorizons();
            if (definition.style() == CirrusConfig.CloudStyle.FANCY) {
                mesh.buffer.drawWithShader(
                        poseStack.last().pose(),
                        projectionMatrix,
                        shader,
                        CirrusShader.DrawMode.DEPTH_ONLY
                );
            }
            mesh.buffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            if (Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency()
                    && !renderingForDistantHorizons) {
                mesh.buffer.drawWithShader(
                        poseStack.last().pose(),
                        projectionMatrix,
                        shader,
                        CirrusShader.DrawMode.MAIN_DEPTH_ONLY
                );
            }
        } finally {
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
        return CirrusEasing.smoothstep(blend);
    }

    private static float smoothWeatherLevel(float level) {
        return CirrusEasing.smoothstep(level);
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
        LightningBolt nearestLightning = CirrusLightningLocator.nearestHorizontal(
                level, cameraX, cameraZ
        );
        float intensity = nearestLightning == null
                || CirrusConfig.HIDE_LIGHTNING_CLOUD_FLASHES.get()
                || Minecraft.getInstance().options.hideLightningFlash().get()
                ? 0.0F
                : Mth.clamp(1.0F - (nearestLightning.tickCount + partialTick) / 3.0F, 0.0F, 1.0F)
                        * CirrusConfig.LIGHTNING_CLOUD_FLASH_OPACITY.get().floatValue();
        Vector3f viewUp = frustumMatrix.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F)).normalize();
        float radius = Mth.clamp(cloudRenderDistance * 0.70F, 320.0F, 768.0F);
        if (intensity <= 0.0F || nearestLightning == null) {
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
            CirrusShader shader,
            float celestialAngle,
            float sunWeight,
            Vector3f celestialViewDirection,
            float rainLevel,
            float thunderLevel,
            LightningState lightning
    ) {
        CirrusShaderUniforms.setUniform(shader, "CirrusEnabled", true);
        CirrusUniform lightDirection = shader.getUniform("CirrusLightDirection");
        if (lightDirection == null) {
            return;
        }
        lightDirection.set(-Mth.sin(celestialAngle), 0.0F);
        CirrusShaderUniforms.setUniform(shader, "CirrusSunWeight", sunWeight);
        CirrusShaderUniforms.setUniform(
                shader, "CirrusLightViewDirection",
                celestialViewDirection.x, celestialViewDirection.y, celestialViewDirection.z,
                0.0F
        );
        CirrusShaderUniforms.setUniform(shader, "CirrusRainLevel", rainLevel);
        CirrusShaderUniforms.setUniform(shader, "CirrusThunderLevel", thunderLevel);
        CirrusShaderUniforms.setUniform(
                shader,
                "CirrusRainCloudCoverage",
                rainLevel * CirrusConfig.RAIN_CLOUD_COVERAGE.get().floatValue()
        );
        CirrusShaderUniforms.setUniform(
                shader,
                "CirrusThunderCloudCoverage",
                thunderLevel * CirrusConfig.THUNDER_CLOUD_COVERAGE.get().floatValue()
        );
        CirrusShaderUniforms.setUniform(shader, "CirrusLightningFlash", lightning.intensity());
        CirrusShaderUniforms.setUniform(
                shader, "CirrusLightningViewPosition",
                lightning.viewPosition().x, lightning.viewPosition().y, lightning.viewPosition().z,
                0.0F
        );
        CirrusShaderUniforms.setUniform(
                shader, "CirrusWorldUpViewDirection",
                lightning.viewUp().x, lightning.viewUp().y, lightning.viewUp().z,
                0.0F
        );
        CirrusShaderUniforms.setUniform(shader, "CirrusLightningRadius", lightning.radius());
    }

    private MeshData buildMesh(
            ByteBufferBuilder sourceBuffer,
            int distanceChunks,
            int anchorX,
            int anchorZ,
            CirrusConfig.CloudStyle style,
            boolean shaderPackDistanceFade
    ) {
        BufferBuilder bufferBuilder = new BufferBuilder(
                sourceBuffer,
                PrimitiveTopology.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );
        float radius = distanceChunks * 16.0F / WORLD_SCALE;
        CloudMeshBuilder builder = new CloudMeshBuilder(bufferBuilder, radius, shaderPackDistanceFade);
        float detailedRadius = Math.min(
                radius,
                CirrusConfig.CLOUD_RENDER_DISTANCE_SETTING.maximum() * 16.0F / WORLD_SCALE
        );
        int tileRadius = Mth.ceil(detailedRadius / TILE_SIZE) + 1;
        float inclusionRadius = detailedRadius + TILE_SIZE * 0.7072F;
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
        if (radius > detailedRadius) {
            addDistantCloudRing(builder, detailedRadius, radius, style, anchorX, anchorZ);
        }
        return bufferBuilder.buildOrThrow();
    }

    private static void addDistantCloudRing(
            CloudMeshBuilder builder,
            float innerRadius,
            float outerRadius,
            CirrusConfig.CloudStyle style,
            int anchorX,
            int anchorZ
    ) {
        float surfaceHeight = style == CirrusConfig.CloudStyle.FANCY
                ? FANCY_CLOUD_THICKNESS - SURFACE_EPSILON * 2.0F
                : -SURFACE_EPSILON;
        float innerX0 = innerRadius;
        float innerZ0 = 0.0F;
        float outerX0 = outerRadius;
        float outerZ0 = 0.0F;
        for (int segment = 1; segment <= DISTANT_RING_SEGMENTS; segment++) {
            float angle = (float)(Math.PI * 2.0 * segment / DISTANT_RING_SEGMENTS);
            float cosine = Mth.cos(angle);
            float sine = Mth.sin(angle);
            float innerX1 = cosine * innerRadius;
            float innerZ1 = sine * innerRadius;
            float outerX1 = cosine * outerRadius;
            float outerZ1 = sine * outerRadius;

            vertex(builder, innerX0, surfaceHeight, innerZ0,
                    innerX0, innerZ0, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
            vertex(builder, innerX1, surfaceHeight, innerZ1,
                    innerX1, innerZ1, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
            vertex(builder, outerX1, surfaceHeight, outerZ1,
                    outerX1, outerZ1, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);
            vertex(builder, outerX0, surfaceHeight, outerZ0,
                    outerX0, outerZ0, anchorX, anchorZ, 1.0F, 0.0F, 1.0F, 0.0F);

            innerX0 = innerX1;
            innerZ0 = innerZ1;
            outerX0 = outerX1;
            outerZ0 = outerZ1;
        }
    }

    private static void addFastTile(
            CloudMeshBuilder builder,
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
            CloudMeshBuilder builder,
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

        vertex(builder, x, 0.0F, z1, x, z1, anchorX, anchorZ, 1.0F, 0.0F, -1.0F, 0.0F);
        vertex(builder, x1, 0.0F, z1, x1, z1, anchorX, anchorZ, 1.0F, 0.0F, -1.0F, 0.0F);
        vertex(builder, x1, 0.0F, z, x1, z, anchorX, anchorZ, 1.0F, 0.0F, -1.0F, 0.0F);
        vertex(builder, x, 0.0F, z, x, z, anchorX, anchorZ, 1.0F, 0.0F, -1.0F, 0.0F);

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
            CloudMeshBuilder builder,
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
        builder.buffer.addVertex(x, y, z)
                .setUv((sampleX + anchorX) * UV_SCALE, (sampleZ + anchorZ) * UV_SCALE)
                .setColor(shade, shade, shade, builder.alpha(x, z))
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

    private static final class CloudMeshBuilder {
        private final BufferBuilder buffer;
        private final float fadeStart;
        private final float fadeEnd;

        private CloudMeshBuilder(BufferBuilder buffer, float radius, boolean distanceFade) {
            this.buffer = buffer;
            this.fadeEnd = distanceFade ? radius : 0.0F;
            this.fadeStart = distanceFade
                    ? radius - Math.max(32.0F / WORLD_SCALE, radius * 0.15F)
                    : 0.0F;
        }

        private float alpha(float x, float z) {
            if (fadeEnd <= fadeStart) {
                return 1.0F;
            }
            float distance = (float)Math.sqrt(x * x + z * z);
            return Mth.clamp((fadeEnd - distance) / (fadeEnd - fadeStart), 0.0F, 1.0F);
        }
    }

    private static final class LayerMesh {
        private CirrusVertexBuffer buffer;
        private ClientLevel cachedLevel;
        private int cachedDistanceChunks = -1;
        private CirrusConfig.CloudStyle cachedStyle;
        private boolean cachedShaderPackDistanceFade;
        private int cachedAnchorX = Integer.MIN_VALUE;
        private int cachedAnchorZ = Integer.MIN_VALUE;

        private void invalidate() {
            closeBuffer();
            cachedLevel = null;
            cachedDistanceChunks = -1;
            cachedStyle = null;
            cachedShaderPackDistanceFade = false;
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
