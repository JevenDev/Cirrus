package com.jvn.cirrus.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.Optional;
import java.util.OptionalDouble;
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
        drawWithShader(modelView, projection, shader, CirrusShader.DrawMode.COLOR);
    }

    public void drawWithShader(
            Matrix4f modelView,
            Matrix4f projection,
            CirrusShader shader,
            CirrusShader.DrawMode mode
    ) {
        if (vertexBuffer == null || drawState == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = minecraft.gameRenderer.mainRenderTarget();
        if (shader.target() == CirrusShader.Target.CLOUDS
                && mode != CirrusShader.DrawMode.MAIN_DEPTH_ONLY) {
            RenderTarget cloudsTarget = minecraft.levelRenderer.cloudsTarget();
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
                             Optional.empty(),
                             depth,
                             OptionalDouble.empty()
                     )) {
            pass.setPipeline(shader.pipeline(mode));
            pass.setUniform("CirrusMatrices", matrices);
            if (parameters != null) {
                pass.setUniform("CirrusParams", parameters);
            }
            if (shader.texture() != null) {
                AbstractTexture texture = minecraft.getTextureManager().getTexture(shader.texture());
                pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            }
            pass.setVertexBuffer(0, vertexBuffer.slice());
            if (indexBuffer != null) {
                pass.setIndexBuffer(indexBuffer, drawState.indexType());
            } else {
                RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
                pass.setIndexBuffer(sequential.getBuffer(drawState.indexCount()), sequential.type());
            }
            pass.drawIndexed(drawState.indexCount(), 1, 0, 0, 0);
        }
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
