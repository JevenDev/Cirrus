package com.jvn.cirrus.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix4f;

public final class CirrusVertexBuffer implements AutoCloseable {
    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;
    private MeshData.DrawState drawState;

    public void bind() {
    }

    public static void unbind() {
    }

    public void upload(MeshData mesh) {
        close();
        try (mesh) {
            drawState = mesh.drawState();
            vertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Cirrus vertex buffer",
                    GpuBuffer.USAGE_VERTEX,
                    mesh.vertexBuffer()
            );
            if (mesh.indexBuffer() != null) {
                indexBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "Cirrus index buffer",
                        GpuBuffer.USAGE_INDEX,
                        mesh.indexBuffer()
                );
            }
        } catch (RuntimeException exception) {
            close();
            throw exception;
        }
    }

    public void drawWithShader(Matrix4f modelView, Matrix4f projection, CirrusShader shader) {
        drawWithShader(modelView, projection, shader, CirrusShader.DrawMode.COLOR, null, null);
    }

    public void drawWithShader(
            Matrix4f modelView,
            Matrix4f projection,
            CirrusShader shader,
            AbstractTexture textureOverride
    ) {
        drawWithShader(
                modelView, projection, shader, CirrusShader.DrawMode.COLOR, null, textureOverride
        );
    }

    public void drawWithShader(
            Matrix4f modelView,
            Matrix4f projection,
            CirrusShader shader,
            ScissorBox scissor
    ) {
        drawWithShader(modelView, projection, shader, CirrusShader.DrawMode.COLOR, scissor, null);
    }

    public void drawWithShader(
            Matrix4f modelView,
            Matrix4f projection,
            CirrusShader shader,
            CirrusShader.DrawMode mode
    ) {
        drawWithShader(modelView, projection, shader, mode, null, null);
    }

    public void drawWithShader(
            Matrix4f modelView,
            Matrix4f projection,
            CirrusShader shader,
            CirrusShader.DrawMode mode,
            AbstractTexture textureOverride
    ) {
        drawWithShader(modelView, projection, shader, mode, null, textureOverride);
    }

    private void drawWithShader(
            Matrix4f modelView,
            Matrix4f projection,
            CirrusShader shader,
            CirrusShader.DrawMode mode,
            ScissorBox scissor,
            AbstractTexture textureOverride
    ) {
        if (vertexBuffer == null || drawState == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.getMainRenderTarget();
        if (shader.target() == CirrusShader.Target.CLOUDS
                && mode != CirrusShader.DrawMode.MAIN_DEPTH_ONLY) {
            RenderTarget cloudsTarget = minecraft.levelRenderer.getCloudsTarget();
            if (cloudsTarget != null) {
                target = cloudsTarget;
            }
        }

        GpuTextureView color = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride
                : target.getColorTextureView();
        GpuTextureView depth = RenderSystem.outputDepthTextureOverride != null
                ? RenderSystem.outputDepthTextureOverride
                : target.getDepthTextureView();

        try (GpuBuffer matrices = shader.createMatricesBuffer(modelView, projection);
             GpuBuffer parameters = shader.createUniformBuffer();
             RenderPass pass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                             () -> "Cirrus custom sky",
                             color,
                             OptionalInt.empty(),
                             depth,
                             OptionalDouble.empty()
                     )) {
            pass.setPipeline(shader.pipeline(mode));
            if (scissor != null) {
                pass.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
            }
            pass.setUniform("CirrusMatrices", matrices);
            if (parameters != null) {
                pass.setUniform("CirrusParams", parameters);
            }
            if (textureOverride != null || shader.texture() != null) {
                AbstractTexture texture = textureOverride != null
                        ? textureOverride
                        : minecraft.getTextureManager().getTexture(shader.texture());
                pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            }
            pass.setVertexBuffer(0, vertexBuffer);
            if (indexBuffer != null) {
                pass.setIndexBuffer(indexBuffer, drawState.indexType());
            } else {
                RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(drawState.mode());
                pass.setIndexBuffer(sequential.getBuffer(drawState.indexCount()), sequential.type());
            }
            pass.drawIndexed(0, 0, drawState.indexCount(), 1);
        }
    }

    public record ScissorBox(int x, int y, int width, int height) {
    }

    @Override
    public void close() {
        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
        if (indexBuffer != null) {
            indexBuffer.close();
            indexBuffer = null;
        }
        drawState = null;
    }
}
