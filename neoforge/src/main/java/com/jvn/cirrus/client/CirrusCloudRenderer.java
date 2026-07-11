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

    private VertexBuffer cloudBuffer;
    private ClientLevel cachedLevel;
    private CloudStatus cachedMode;
    private int cachedDistanceChunks = -1;
    private int cachedAnchorX = Integer.MIN_VALUE;
    private int cachedAnchorZ = Integer.MIN_VALUE;

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
            CloudStatus mode
    ) {
        float cloudHeight = level.effects().getCloudHeight();
        if (mode == CloudStatus.OFF || Float.isNaN(cloudHeight)) {
            return;
        }

        int distanceChunks = CirrusConfig.CLOUD_RENDER_DISTANCE.get();
        double wind = (ticks + partialTick) * 0.03;
        double sampleX = (cameraX + wind) / WORLD_SCALE;
        double sampleZ = cameraZ / WORLD_SCALE;
        sampleX -= Mth.floor(sampleX / 2048.0) * 2048.0;
        sampleZ -= Mth.floor(sampleZ / 2048.0) * 2048.0;

        int anchorX = Mth.floor(sampleX / TILE_SIZE) * TILE_SIZE;
        int anchorZ = Mth.floor(sampleZ / TILE_SIZE) * TILE_SIZE;
        ensureMesh(level, mode, distanceChunks, anchorX, anchorZ);
        if (cloudBuffer == null) {
            return;
        }

        float oldFogStart = RenderSystem.getShaderFogStart();
        float oldFogEnd = RenderSystem.getShaderFogEnd();
        FogShape oldFogShape = RenderSystem.getShaderFogShape();
        float distanceBlocks = distanceChunks * 16.0F;
        float fadeLength = Math.max(32.0F, distanceBlocks * 0.15F);
        Vec3 cloudColor = level.getCloudColor(partialTick);

        poseStack.pushPose();
        try {
            FogRenderer.levelFogColor();
            RenderSystem.setShaderFogStart(Math.max(0.0F, distanceBlocks - fadeLength));
            RenderSystem.setShaderFogEnd(distanceBlocks);
            RenderSystem.setShaderFogShape(FogShape.CYLINDER);
            RenderSystem.setShaderColor((float)cloudColor.x, (float)cloudColor.y, (float)cloudColor.z, 1.0F);

            poseStack.mulPose(frustumMatrix);
            poseStack.scale(WORLD_SCALE, 1.0F, WORLD_SCALE);
            poseStack.translate(
                    -(sampleX - anchorX),
                    cloudHeight - cameraY + 0.33,
                    -(sampleZ - anchorZ)
            );

            cloudBuffer.bind();
            int firstPass = mode == CloudStatus.FANCY ? 0 : 1;
            for (int pass = firstPass; pass < 2; pass++) {
                RenderType renderType = pass == 0 ? RenderType.cloudsDepthOnly() : RenderType.clouds();
                renderType.setupRenderState();
                try {
                    ShaderInstance shader = RenderSystem.getShader();
                    cloudBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
                } finally {
                    renderType.clearRenderState();
                }
            }
            VertexBuffer.unbind();
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderFogStart(oldFogStart);
            RenderSystem.setShaderFogEnd(oldFogEnd);
            RenderSystem.setShaderFogShape(oldFogShape);
            poseStack.popPose();
        }
    }

    private void ensureMesh(ClientLevel level, CloudStatus mode, int distanceChunks, int anchorX, int anchorZ) {
        if (cloudBuffer != null
                && cachedLevel == level
                && cachedMode == mode
                && cachedDistanceChunks == distanceChunks
                && cachedAnchorX == anchorX
                && cachedAnchorZ == anchorZ) {
            return;
        }

        closeBuffer();
        MeshData mesh = buildMesh(mode, distanceChunks, anchorX, anchorZ);
        cloudBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        cloudBuffer.bind();
        cloudBuffer.upload(mesh);
        VertexBuffer.unbind();
        cachedLevel = level;
        cachedMode = mode;
        cachedDistanceChunks = distanceChunks;
        cachedAnchorX = anchorX;
        cachedAnchorZ = anchorZ;
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
                .setColor(red, green, blue, 0.8F)
                .setNormal(normalX, normalY, normalZ);
    }

    public void invalidate() {
        closeBuffer();
        cachedLevel = null;
        cachedMode = null;
        cachedDistanceChunks = -1;
        cachedAnchorX = Integer.MIN_VALUE;
        cachedAnchorZ = Integer.MIN_VALUE;
    }

    private void closeBuffer() {
        if (cloudBuffer != null) {
            cloudBuffer.close();
            cloudBuffer = null;
        }
    }

    @Override
    public void close() {
        invalidate();
    }
}
