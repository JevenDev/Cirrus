package com.jvn.cirrus.client.render;

import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class CirrusShader {
    private final String name;
    private final Target target;
    private final ResourceLocation texture;
    private final Map<String, CirrusUniform> uniforms = new LinkedHashMap<>();
    private final RenderPipeline colorPipeline;
    private final RenderPipeline depthPipeline;
    private final RenderPipeline reversedColorPipeline;
    private final RenderPipeline reversedDepthPipeline;
    private final int uniformBufferSize;
    private DynamicUniformStorage<Matrices> matrices;
    private DynamicUniformStorage<Parameters> parameters;

    public CirrusShader(
            String name,
            VertexFormat vertexFormat,
            Blend blend,
            Target target,
            ResourceLocation texture,
            boolean colorWrite,
            boolean depthWrite,
            UniformSpec... specs
    ) {
        this.name = name;
        this.target = target;
        this.texture = texture;

        int size = 0;
        for (UniformSpec spec : specs) {
            size = align(size, spec.alignment());
            size += spec.byteSize();
            uniforms.put(spec.name(), new CirrusUniform(spec.name(), spec.components(), spec.defaults()));
        }
        this.uniformBufferSize = align(size, 16);
        this.colorPipeline = createPipeline(
                name, vertexFormat, blend, texture != null, colorWrite, depthWrite, false,
                DepthTestFunction.LEQUAL_DEPTH_TEST
        );
        this.depthPipeline = createPipeline(
                name + "_depth", vertexFormat, Blend.NONE, texture != null, false, true, true,
                DepthTestFunction.LEQUAL_DEPTH_TEST
        );
        this.reversedColorPipeline = target == Target.CLOUDS ? createPipeline(
                name + "_dh_reversed", vertexFormat, blend, texture != null,
                colorWrite, depthWrite, false, DepthTestFunction.EQUAL_DEPTH_TEST
        ) : colorPipeline;
        this.reversedDepthPipeline = target == Target.CLOUDS ? createPipeline(
                name + "_dh_reversed_depth", vertexFormat, Blend.NONE, texture != null,
                false, true, true, DepthTestFunction.GREATER_DEPTH_TEST
        ) : depthPipeline;
    }

    public CirrusUniform getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }

    public ResourceLocation texture() {
        return texture;
    }

    Target target() {
        return target;
    }

    RenderPipeline pipeline(DrawMode mode) {
        if (target == Target.CLOUDS && DistantHorizonsCompat.isRenderingWithReversedDepth()) {
            return mode == DrawMode.COLOR ? reversedColorPipeline : reversedDepthPipeline;
        }
        return mode == DrawMode.COLOR ? colorPipeline : depthPipeline;
    }

    GpuBufferSlice writeMatrices(Matrix4f modelView, Matrix4f projection) {
        if (matrices == null) {
            matrices = new DynamicUniformStorage<>("Cirrus " + name + " matrices", 128, 16);
        }
        return matrices.writeUniform(new Matrices(modelView, projection));
    }

    GpuBufferSlice writeParameters() {
        if (uniformBufferSize == 0) {
            return null;
        }
        if (parameters == null) {
            parameters = new DynamicUniformStorage<>("Cirrus " + name + " parameters", uniformBufferSize, 16);
        }
        return parameters.writeUniform(new Parameters());
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
    private static final class Matrices implements DynamicUniformStorage.DynamicUniform {
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

    private final class Parameters implements DynamicUniformStorage.DynamicUniform {
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

    private RenderPipeline createPipeline(
            String pipelineName,
            VertexFormat vertexFormat,
            Blend blend,
            boolean textured,
            boolean colorWrite,
            boolean depthWrite,
            boolean depthVariant,
            DepthTestFunction depthTest
    ) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Cirrus.id("pipeline/" + pipelineName))
                .withVertexShader(Cirrus.id("core/" + name))
                .withFragmentShader(Cirrus.id("core/" + name))
                .withUniform("CirrusMatrices", UniformType.UNIFORM_BUFFER)
                .withDepthTestFunction(depthTest)
                .withCull(false)
                .withColorWrite(colorWrite)
                .withDepthWrite(depthWrite)
                .withVertexFormat(vertexFormat, VertexFormat.Mode.QUADS);
        if (!uniforms.isEmpty()) {
            builder.withUniform("CirrusParams", UniformType.UNIFORM_BUFFER);
        }
        if (textured) {
            builder.withSampler("Sampler0");
        }
        if (!depthVariant) {
            if (blend == Blend.TRANSLUCENT) {
                builder.withBlend(BlendFunction.TRANSLUCENT);
            } else if (blend == Blend.ADDITIVE) {
                builder.withBlend(BlendFunction.OVERLAY);
            }
        }
        return RenderPipelines.register(builder.build());
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
        DEPTH_ONLY,
        MAIN_DEPTH_ONLY
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
