package com.jvn.cirrus.client;

import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CirrusCloudRenderer implements AutoCloseable {
    private static final float WORLD_SCALE = 12.0F;
    private static final int TILE_SIZE = 8;
    private static final float CLOUD_THICKNESS = 4.0F;
    private static final float UV_SCALE = 1.0F / 256.0F;
    private static final float EDGE_EPSILON = 1.0F / 1024.0F;
    private static final int UPPER_PATTERN_OFFSET_X = 37;
    private static final int UPPER_PATTERN_OFFSET_Z = 91;
    private static final LayerDefinition LOWER_LAYER = new LayerDefinition(false, 0, 0);
    private static final LayerDefinition UPPER_LAYER = new LayerDefinition(true, UPPER_PATTERN_OFFSET_X, UPPER_PATTERN_OFFSET_Z);

    private final LayerMesh lowerMesh = new LayerMesh();
    private final LayerMesh upperMesh = new LayerMesh();

    public void render(
            ClientLevel level,
            PoseStack poseStack,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            int ticks,
            double cameraX,
            double cameraY,
            double cameraZ,
            CloudStatus globalMode
    ) {
        float cloudHeight = level.effects().getCloudHeight();
        if (globalMode == CloudStatus.OFF || Float.isNaN(cloudHeight)) {
            return;
        }

        int distanceChunks = CirrusConfig.CLOUD_RENDER_DISTANCE.get();
        double cameraSampleX = cameraX / WORLD_SCALE;
        double cameraSampleZ = cameraZ / WORLD_SCALE;
        double windSample = (ticks + partialTick) * 0.03 / WORLD_SCALE;
        CloudStatus lowerMode = CirrusConfig.LOWER_LAYER_QUALITY.get().cloudStatus();
        CloudStatus upperMode = CirrusConfig.UPPER_LAYER_QUALITY.get().cloudStatus();

        prepareLayer(lowerMesh, LOWER_LAYER, level, lowerMode, distanceChunks, cameraSampleX, cameraSampleZ, windSample);
        boolean upperEnabled = CirrusConfig.UPPER_LAYER_ENABLED.get();
        if (upperEnabled) {
            prepareLayer(upperMesh, UPPER_LAYER, level, upperMode, distanceChunks, cameraSampleX, cameraSampleZ, windSample);
        } else {
            upperMesh.invalidate();
        }
        if (lowerMesh.buffer == null && upperMesh.buffer == null) {
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

        try {
            FogRenderer.levelFogColor();
            RenderSystem.setShaderFogStart(Math.max(0.0F, distanceBlocks - fadeLength));
            RenderSystem.setShaderFogEnd(distanceBlocks);
            RenderSystem.setShaderFogShape(FogShape.CYLINDER);
            RenderSystem.setShaderColor((float)cloudColor.x, (float)cloudColor.y, (float)cloudColor.z, 1.0F);

            drawLayer(
                    lowerMesh,
                    LOWER_LAYER,
                    poseStack,
                    frustumMatrix,
                    projectionMatrix,
                    lowerMode,
                    cloudHeight + CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get() - cameraY + 0.33,
                    cameraSampleX,
                    cameraSampleZ,
                    windSample,
                    cloudColor
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
                        upperMode,
                        upperHeight,
                        cameraSampleX,
                        cameraSampleZ,
                        windSample,
                        cloudColor
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

    private void prepareLayer(
            LayerMesh mesh,
            LayerDefinition definition,
            ClientLevel level,
            CloudStatus mode,
            int distanceChunks,
            double cameraSampleX,
            double cameraSampleZ,
            double windSample
    ) {
        double sampleX = sampleX(definition, cameraSampleX, windSample);
        double sampleZ = sampleZ(definition, cameraSampleZ);
        int anchorX = Mth.floor(sampleX / TILE_SIZE) * TILE_SIZE;
        int anchorZ = Mth.floor(sampleZ / TILE_SIZE) * TILE_SIZE;
        if (mesh.buffer != null
                && mesh.cachedLevel == level
                && mesh.cachedMode == mode
                && mesh.cachedDistanceChunks == distanceChunks
                && mesh.cachedAnchorX == anchorX
                && mesh.cachedAnchorZ == anchorZ) {
            return;
        }

        mesh.closeBuffer();
        MeshData builtMesh = buildMesh(mode, distanceChunks, anchorX, anchorZ);
        mesh.buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        mesh.buffer.bind();
        mesh.buffer.upload(builtMesh);
        VertexBuffer.unbind();
        mesh.cachedLevel = level;
        mesh.cachedMode = mode;
        mesh.cachedDistanceChunks = distanceChunks;
        mesh.cachedAnchorX = anchorX;
        mesh.cachedAnchorZ = anchorZ;
    }

    private void drawLayer(
            LayerMesh mesh,
            LayerDefinition definition,
            PoseStack poseStack,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            CloudStatus mode,
            double relativeHeight,
            double cameraSampleX,
            double cameraSampleZ,
            double windSample,
            Vec3 cloudColor
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
                    definition.opacity()
            );
            poseStack.mulPose(frustumMatrix);
            poseStack.scale(WORLD_SCALE, 1.0F, WORLD_SCALE);
            poseStack.translate(
                    -(sampleX - mesh.cachedAnchorX),
                    relativeHeight,
                    -(sampleZ - mesh.cachedAnchorZ)
            );

            mesh.buffer.bind();
            int firstPass = mode == CloudStatus.FANCY ? 0 : 1;
            for (int pass = firstPass; pass < 2; pass++) {
                RenderType renderType = pass == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
                renderType.setupRenderState();
                try {
                    ShaderInstance shader = RenderSystem.getShader();
                    mesh.buffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
                } finally {
                    renderType.clearRenderState();
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

    private MeshData buildMesh(CloudStatus mode, int distanceChunks, int anchorX, int anchorZ) {
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
                if (mode == CloudStatus.FANCY) {
                    addFancyTile(builder, x, z, tileX, tileZ, anchorX, anchorZ);
                } else {
                    addHorizontalFace(builder, x, z, 0.0F, 1.0F, 1.0F, 1.0F, anchorX, anchorZ, true);
                }
            }
        }
        return builder.buildOrThrow();
    }

    private static void addFancyTile(
            BufferBuilder builder,
            float x,
            float z,
            int tileX,
            int tileZ,
            int anchorX,
            int anchorZ
    ) {
        addHorizontalFace(builder, x, z, 0.0F, 0.7F, 0.7F, 0.7F, anchorX, anchorZ, false);
        addHorizontalFace(builder, x, z, CLOUD_THICKNESS - EDGE_EPSILON, 1.0F, 1.0F, 1.0F, anchorX, anchorZ, true);

        if (tileX >= 0) {
            addXFace(builder, x, z, -1.0F, 0.9F, anchorX, anchorZ);
        }
        if (tileX <= 0) {
            addXFace(builder, x + TILE_SIZE - EDGE_EPSILON, z, 1.0F, 0.9F, anchorX, anchorZ);
        }
        if (tileZ >= 0) {
            addZFace(builder, x, z, -1.0F, 0.8F, anchorX, anchorZ);
        }
        if (tileZ <= 0) {
            addZFace(builder, x, z + TILE_SIZE - EDGE_EPSILON, 1.0F, 0.8F, anchorX, anchorZ);
        }
    }

    private static void addHorizontalFace(
            BufferBuilder builder,
            float x,
            float z,
            float y,
            float red,
            float green,
            float blue,
            int anchorX,
            int anchorZ,
            boolean up
    ) {
        float x1 = x + TILE_SIZE;
        float z1 = z + TILE_SIZE;
        float normalY = up ? 1.0F : -1.0F;
        if (up) {
            vertex(builder, x, y, z, x, z, red, green, blue, 0.0F, normalY, 0.0F, anchorX, anchorZ);
            vertex(builder, x, y, z1, x, z1, red, green, blue, 0.0F, normalY, 0.0F, anchorX, anchorZ);
            vertex(builder, x1, y, z1, x1, z1, red, green, blue, 0.0F, normalY, 0.0F, anchorX, anchorZ);
            vertex(builder, x1, y, z, x1, z, red, green, blue, 0.0F, normalY, 0.0F, anchorX, anchorZ);
        } else {
            vertex(builder, x, y, z1, x, z1, red, green, blue, 0.0F, normalY, 0.0F, anchorX, anchorZ);
            vertex(builder, x, y, z, x, z, red, green, blue, 0.0F, normalY, 0.0F, anchorX, anchorZ);
            vertex(builder, x1, y, z, x1, z, red, green, blue, 0.0F, normalY, 0.0F, anchorX, anchorZ);
            vertex(builder, x1, y, z1, x1, z1, red, green, blue, 0.0F, normalY, 0.0F, anchorX, anchorZ);
        }
    }

    private static void addXFace(
            BufferBuilder builder,
            float x,
            float z,
            float normalX,
            float shade,
            int anchorX,
            int anchorZ
    ) {
        for (int strip = 0; strip < TILE_SIZE; strip++) {
            float z0 = z + strip;
            float z1 = z0 + 1.0F;
            float sampleZ = z0 + 0.5F;
            vertex(builder, x, 0.0F, z1, x, sampleZ, shade, shade, shade, normalX, 0.0F, 0.0F, anchorX, anchorZ);
            vertex(builder, x, CLOUD_THICKNESS, z1, x, sampleZ, shade, shade, shade, normalX, 0.0F, 0.0F, anchorX, anchorZ);
            vertex(builder, x, CLOUD_THICKNESS, z0, x, sampleZ, shade, shade, shade, normalX, 0.0F, 0.0F, anchorX, anchorZ);
            vertex(builder, x, 0.0F, z0, x, sampleZ, shade, shade, shade, normalX, 0.0F, 0.0F, anchorX, anchorZ);
        }
    }

    private static void addZFace(
            BufferBuilder builder,
            float x,
            float z,
            float normalZ,
            float shade,
            int anchorX,
            int anchorZ
    ) {
        for (int strip = 0; strip < TILE_SIZE; strip++) {
            float x0 = x + strip;
            float x1 = x0 + 1.0F;
            float sampleX = x0 + 0.5F;
            vertex(builder, x0, CLOUD_THICKNESS, z, sampleX, z, shade, shade, shade, 0.0F, 0.0F, normalZ, anchorX, anchorZ);
            vertex(builder, x1, CLOUD_THICKNESS, z, sampleX, z, shade, shade, shade, 0.0F, 0.0F, normalZ, anchorX, anchorZ);
            vertex(builder, x1, 0.0F, z, sampleX, z, shade, shade, shade, 0.0F, 0.0F, normalZ, anchorX, anchorZ);
            vertex(builder, x0, 0.0F, z, sampleX, z, shade, shade, shade, 0.0F, 0.0F, normalZ, anchorX, anchorZ);
        }
    }

    private static void vertex(
            BufferBuilder builder,
            float x,
            float y,
            float z,
            float sampleX,
            float sampleZ,
            float red,
            float green,
            float blue,
            float normalX,
            float normalY,
            float normalZ,
            int anchorX,
            int anchorZ
    ) {
        builder.addVertex(x, y, z)
                .setUv((sampleX + anchorX) * UV_SCALE, (sampleZ + anchorZ) * UV_SCALE)
                .setColor(red, green, blue, 1.0F)
                .setNormal(normalX, normalY, normalZ);
    }

    public void invalidate() {
        lowerMesh.invalidate();
        upperMesh.invalidate();
    }

    @Override
    public void close() {
        invalidate();
    }

    private record LayerDefinition(boolean upper, int patternOffsetX, int patternOffsetZ) {
        private double speed() {
            return upper ? CirrusConfig.UPPER_LAYER_SPEED.get() : CirrusConfig.LOWER_LAYER_SPEED.get();
        }

        private float opacity() {
            return (upper ? CirrusConfig.UPPER_LAYER_OPACITY.get() : CirrusConfig.LOWER_LAYER_OPACITY.get()).floatValue();
        }
    }

    private static final class LayerMesh {
        private VertexBuffer buffer;
        private ClientLevel cachedLevel;
        private CloudStatus cachedMode;
        private int cachedDistanceChunks = -1;
        private int cachedAnchorX = Integer.MIN_VALUE;
        private int cachedAnchorZ = Integer.MIN_VALUE;

        private void invalidate() {
            closeBuffer();
            cachedLevel = null;
            cachedMode = null;
            cachedDistanceChunks = -1;
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
