package com.jvn.cirrus.client.render;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix4f;

public final class CirrusVertexBuffer implements AutoCloseable {
    private GpuBuffer vertexBuffer;
    private GpuBuffer indexBuffer;
    private MeshData.DrawState drawState;

    public void upload(MeshData mesh, ByteBufferBuilder sourceBuffer) {
        try (sourceBuffer) {
            upload(mesh);
        }
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

        if (CirrusRenderContext.oitStage() != null && mode != CirrusShader.DrawMode.COLOR) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        RenderPass pass = Objects.requireNonNull(CirrusRenderContext.renderPass());
        GpuBufferSlice matrices = shader.writeMatrices(modelView, projection);
        GpuBufferSlice parameters = shader.writeParameters();
        pass.setPipeline(RenderSystem.getCompiledPipeline(shader.pipeline(mode)));
        if (shader.usesVanillaFog()) {
            pass.setUniform("Fog", RenderSystem.getShaderFog());
        }
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
            pass.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());
        }
        pass.setVertexBuffer(0, vertexBuffer.slice());
        if (indexBuffer != null) {
            pass.setIndexBuffer(indexBuffer, drawState.indexType());
        } else {
            RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
            pass.setIndexBuffer(sequential.getBuffer(drawState.indexCount()), sequential.type());
        }
        pass.drawIndexed(drawState.indexCount(), 1, 0, 0, 0);
        if (scissor != null) {
            pass.disableScissor();
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
