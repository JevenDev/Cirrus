package com.jvn.cirrus.client.util;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public record CirrusSunScissor(int x, int y, int width, int height) {
    private static final int PADDING = 2;
    private static final float MIN_CLIP_W = 1.0E-4F;

    public static CirrusSunScissor forMesh(
            MeshData mesh,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            int framebufferWidth,
            int framebufferHeight
    ) {
        if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            return null;
        }
        VertexFormat format = mesh.drawState().format();
        int positionOffset = format.getOffset(VertexFormatElement.POSITION);
        if (positionOffset < 0) {
            return new CirrusSunScissor(0, 0, framebufferWidth, framebufferHeight);
        }
        // the mesh already includes celestial size, orbit and pose transformations
        Matrix4f clipTransform = new Matrix4f(projectionMatrix).mul(modelViewMatrix);
        float minimumX = Float.POSITIVE_INFINITY;
        float minimumY = Float.POSITIVE_INFINITY;
        float maximumX = Float.NEGATIVE_INFINITY;
        float maximumY = Float.NEGATIVE_INFINITY;
        boolean hasFrontVertex = false;
        boolean hasBehindVertex = false;

        ByteBuffer vertices = mesh.vertexBuffer();
        int stride = format.getVertexSize();
        Vector4f clip = new Vector4f();
        for (int vertex = 0; vertex < mesh.drawState().vertexCount(); vertex++) {
            int offset = vertex * stride + positionOffset;
            clipTransform.transform(clip.set(
                    vertices.getFloat(offset),
                    vertices.getFloat(offset + Float.BYTES),
                    vertices.getFloat(offset + 2 * Float.BYTES),
                    1.0F
            ));
            if (!clip.isFinite()) {
                return new CirrusSunScissor(0, 0, framebufferWidth, framebufferHeight);
            }
            if (clip.w <= MIN_CLIP_W) {
                hasBehindVertex = true;
                continue;
            }
            float inverseW = 1.0F / clip.w;
            float x = clip.x * inverseW;
            float y = clip.y * inverseW;
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                return new CirrusSunScissor(0, 0, framebufferWidth, framebufferHeight);
            }
            hasFrontVertex = true;
            minimumX = Math.min(minimumX, x);
            minimumY = Math.min(minimumY, y);
            maximumX = Math.max(maximumX, x);
            maximumY = Math.max(maximumY, y);
        }

        if (!hasFrontVertex) {
            return null;
        }
        if (hasBehindVertex) {
            return new CirrusSunScissor(0, 0, framebufferWidth, framebufferHeight);
        }

        float visibleMinimumX = Math.max(-1.0F, minimumX);
        float visibleMinimumY = Math.max(-1.0F, minimumY);
        float visibleMaximumX = Math.min(1.0F, maximumX);
        float visibleMaximumY = Math.min(1.0F, maximumY);
        if (visibleMaximumX <= visibleMinimumX || visibleMaximumY <= visibleMinimumY) {
            return null;
        }

        int x = Math.max(0, Mth.floor((visibleMinimumX * 0.5F + 0.5F) * framebufferWidth)
                - PADDING);
        int y = Math.max(0, Mth.floor((visibleMinimumY * 0.5F + 0.5F) * framebufferHeight)
                - PADDING);
        int maximumPixelX = Math.min(
                framebufferWidth,
                Mth.ceil((visibleMaximumX * 0.5F + 0.5F) * framebufferWidth) + PADDING
        );
        int maximumPixelY = Math.min(
                framebufferHeight,
                Mth.ceil((visibleMaximumY * 0.5F + 0.5F) * framebufferHeight) + PADDING
        );
        return new CirrusSunScissor(x, y, maximumPixelX - x, maximumPixelY - y);
    }
}
