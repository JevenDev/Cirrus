package com.jvn.cirrus.client.render;

import com.jvn.cirrus.Cirrus;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class CirrusShader {
    private final String name;
    private final Identifier texture;
    private final Map<String, CirrusUniform> uniforms = new LinkedHashMap<>();
    private final boolean usesVanillaFog;
    private final RenderPipeline colorPipeline;
    private final RenderPipeline depthPipeline;
    private final OitPipelineSet oitPipelines;
    private final int uniformBufferSize;
    private DynamicGpuDataStorageMapped<Matrices> matrices;
    private DynamicGpuDataStorageMapped<Parameters> parameters;

    public CirrusShader(
            String name,
            VertexFormat vertexFormat,
            Blend blend,
            Target target,
            Identifier texture,
            boolean colorWrite,
            boolean depthWrite,
            UniformSpec... specs
    ) {
        this(name, vertexFormat, blend, target, texture, colorWrite, depthWrite, false, specs);
    }

    public CirrusShader(
            String name,
            VertexFormat vertexFormat,
            Blend blend,
            Target target,
            Identifier texture,
            boolean colorWrite,
            boolean depthWrite,
            boolean usesVanillaFog,
            UniformSpec... specs
    ) {
        this.name = name;
        this.texture = texture;
        this.usesVanillaFog = usesVanillaFog;

        int size = 0;
        for (UniformSpec spec : specs) {
            size = align(size, spec.alignment());
            size += spec.byteSize();
            uniforms.put(spec.name(), new CirrusUniform(spec.name(), spec.components(), spec.defaults()));
        }
        this.uniformBufferSize = align(size, 16);
        this.colorPipeline = RenderPipelines.register(createPipeline(
                name, vertexFormat, blend, texture != null, colorWrite, depthWrite, usesVanillaFog, false
        ).build());
        this.depthPipeline = RenderPipelines.register(createPipeline(
                name + "_depth", vertexFormat, Blend.NONE, texture != null, false, true, usesVanillaFog, true
        ).build());
        this.oitPipelines = target == Target.CLOUDS ? RenderPipelines.register(
                OitPipelineSet.builder("cirrus_" + name, createPipeline(
                        name, vertexFormat, blend, texture != null, colorWrite, false, usesVanillaFog, false
                ).withUnusedColorTargetState(0)).build()
        ) : null;
    }

    public CirrusUniform getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }

    public Identifier texture() {
        return texture;
    }

    boolean usesVanillaFog() {
        return usesVanillaFog;
    }

    RenderPipeline pipeline(DrawMode mode) {
        if (oitPipelines != null && CirrusRenderContext.oitStage() != null) {
            return oitPipelines.getPipeline(CirrusRenderContext.oitStage());
        }
        return mode == DrawMode.COLOR ? colorPipeline : depthPipeline;
    }

    GpuBufferSlice writeMatrices(Matrix4f modelView, Matrix4f projection) {
        if (matrices == null) {
            matrices = new DynamicGpuDataStorageMapped<>("Cirrus " + name + " matrices", 128, GpuBuffer.USAGE_UNIFORM, 16);
        }
        return matrices.writeData(new Matrices(modelView, projection));
    }

    GpuBufferSlice writeParameters() {
        if (uniformBufferSize == 0) {
            return null;
        }
        if (parameters == null) {
            parameters = new DynamicGpuDataStorageMapped<>("Cirrus " + name + " parameters", uniformBufferSize, GpuBuffer.USAGE_UNIFORM, 16);
        }
        return parameters.writeData(new Parameters());
    }

    public void endFrame() {
        if (matrices != null) {
            matrices.endFrame();
        }
        if (parameters != null) {
            parameters.endFrame();
        }
    }

    public void closeBuffers() {
        if (matrices != null) {
            matrices.close();
            matrices = null;
        }
        if (parameters != null) {
            parameters.close();
            parameters = null;
        }
    }

    // storage writes immediately and compares identities so mutable inputs never reuse stale data
    private static final class Matrices implements DynamicGpuDataStorage.DynamicGpuData {
        private final Matrix4f modelView;
        private final Matrix4f projection;

        private Matrices(Matrix4f modelView, Matrix4f projection) {
            this.modelView = modelView;
            this.projection = projection;
        }

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer).putMat4f(modelView).putMat4f(projection);
        }
    }

    private final class Parameters implements DynamicGpuDataStorage.DynamicGpuData {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder builder = Std140Builder.intoBuffer(buffer);
            for (CirrusUniform uniform : uniforms.values()) {
                float[] values = uniform.values();
                switch (uniform.components()) {
                    case 1 -> builder.putFloat(values[0]);
                    case 2 -> builder.putVec2(values[0], values[1]);
                    case 3 -> builder.putVec3(values[0], values[1], values[2]);
                    case 4 -> builder.putVec4(values[0], values[1], values[2], values[3]);
                    default -> throw new IllegalStateException("Unsupported uniform size");
                }
            }
            builder.align(16).get();
        }
    }

    private RenderPipeline.Builder createPipeline(
            String pipelineName,
            VertexFormat vertexFormat,
            Blend blend,
            boolean textured,
            boolean colorWrite,
            boolean depthWrite,
            boolean usesVanillaFog,
            boolean depthVariant
    ) {
        BindGroupLayout.Builder bindGroupLayout = BindGroupLayout.builder()
                .withUniform("CirrusMatrices", UniformType.UNIFORM_BUFFER);
        if (usesVanillaFog) {
            bindGroupLayout.withUniform("Fog", UniformType.UNIFORM_BUFFER);
        }
        if (!uniforms.isEmpty()) {
            bindGroupLayout.withUniform("CirrusParams", UniformType.UNIFORM_BUFFER);
        }
        if (textured) {
            bindGroupLayout.withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER);
        }
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Cirrus.id("pipeline/" + pipelineName))
                .withVertexShader(Cirrus.id("core/" + name))
                .withFragmentShader(Cirrus.id("core/" + name))
                .withBindGroupLayout(bindGroupLayout.build())
                .withCull(false)
                .withVertexBinding(0, vertexFormat)
                .withPrimitiveTopology(PrimitiveTopology.QUADS);

        BlendFunction blendFunction = null;
        if (!depthVariant) {
            if (blend == Blend.TRANSLUCENT) {
                blendFunction = BlendFunction.TRANSLUCENT;
            } else if (blend == Blend.ADDITIVE) {
                blendFunction = BlendFunction.OVERLAY;
            }
        }
        builder.withColorTargetState(new ColorTargetState(
                Optional.ofNullable(blendFunction),
                GpuFormat.RGBA8_UNORM,
                colorWrite ? ColorTargetState.WRITE_ALL : ColorTargetState.WRITE_NONE
        ));
        builder.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, depthWrite));
        return builder;
    }

    private static int align(int value, int alignment) {
        return (value + alignment - 1) / alignment * alignment;
    }

    public static UniformSpec uniform(String name, int components, float... defaults) {
        return new UniformSpec(name, components, defaults);
    }

    public enum Blend {
        NONE,
        TRANSLUCENT,
        ADDITIVE
    }

    public enum Target {
        MAIN,
        CLOUDS
    }

    public enum DrawMode {
        COLOR,
        DEPTH_ONLY
    }

    public record UniformSpec(String name, int components, float[] defaults) {
        public UniformSpec {
            if (components < 1 || components > 4 || defaults.length != components) {
                throw new IllegalArgumentException("Invalid Cirrus uniform " + name);
            }
        }

        int alignment() {
            return components == 1 ? 4 : components == 2 ? 8 : 16;
        }

        int byteSize() {
            return components <= 2 ? components * 4 : 16;
        }
    }
}
