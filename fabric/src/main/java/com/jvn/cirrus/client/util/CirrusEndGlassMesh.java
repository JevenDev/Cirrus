package com.jvn.cirrus.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.jvn.cirrus.client.render.CirrusVertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CirrusEndGlassMesh {
    private CirrusEndGlassMesh() {
    }

    public static CirrusVertexBuffer create(float radius) {
        return create(radius, false);
    }

    public static CirrusVertexBuffer createBeams(float radius) {
        return create(radius, true);
    }

    private static CirrusVertexBuffer create(float radius, boolean beams) {
        float goldenRatio = (1.0F + (float)Math.sqrt(5.0)) / 2.0F;
        List<Vector3f> vertices = new ArrayList<>(List.of(
                new Vector3f(-1, goldenRatio, 0), new Vector3f(1, goldenRatio, 0),
                new Vector3f(-1, -goldenRatio, 0), new Vector3f(1, -goldenRatio, 0),
                new Vector3f(0, -1, goldenRatio), new Vector3f(0, 1, goldenRatio),
                new Vector3f(0, -1, -goldenRatio), new Vector3f(0, 1, -goldenRatio),
                new Vector3f(goldenRatio, 0, -1), new Vector3f(goldenRatio, 0, 1),
                new Vector3f(-goldenRatio, 0, -1), new Vector3f(-goldenRatio, 0, 1)
        ));
        vertices.forEach(Vector3f::normalize);
        List<int[]> faces = new ArrayList<>(List.of(
                new int[]{0, 11, 5}, new int[]{0, 5, 1}, new int[]{0, 1, 7}, new int[]{0, 7, 10},
                new int[]{0, 10, 11}, new int[]{1, 5, 9}, new int[]{5, 11, 4}, new int[]{11, 10, 2},
                new int[]{10, 7, 6}, new int[]{7, 1, 8}, new int[]{3, 9, 4}, new int[]{3, 4, 2},
                new int[]{3, 2, 6}, new int[]{3, 6, 8}, new int[]{3, 8, 9}, new int[]{4, 9, 5},
                new int[]{2, 4, 11}, new int[]{6, 2, 10}, new int[]{8, 6, 7}, new int[]{9, 8, 1}
        ));
        for (int subdivision = 0; subdivision < 2; subdivision++) {
            Map<Long, Integer> midpoints = new HashMap<>();
            List<int[]> divided = new ArrayList<>();
            for (int[] face : faces) {
                int a = midpoint(vertices, midpoints, face[0], face[1]);
                int b = midpoint(vertices, midpoints, face[1], face[2]);
                int c = midpoint(vertices, midpoints, face[2], face[0]);
                divided.add(new int[]{face[0], a, c});
                divided.add(new int[]{face[1], b, a});
                divided.add(new int[]{face[2], c, b});
                divided.add(new int[]{a, b, c});
            }
            faces = divided;
        }
        RandomSource random = RandomSource.create(0xE0D61A55L);
        // shared vertices keep the shell sealed until the individual shards move
        for (Vector3f vertex : vertices) {
            vertex.add(
                    (random.nextFloat() - 0.5F) * 0.16F,
                    (random.nextFloat() - 0.5F) * 0.16F,
                    (random.nextFloat() - 0.5F) * 0.16F
            ).normalize().mul(radius);
        }
        List<int[]> shards = new ArrayList<>();
        for (int[] face : faces) {
            if (random.nextFloat() < 0.38F) {
                float weightA = 0.12F + random.nextFloat() * 0.30F;
                float weightB = 0.12F + random.nextFloat() * 0.30F;
                int center = vertices.size();
                vertices.add(new Vector3f(vertices.get(face[0])).mul(weightA)
                        .fma(weightB, vertices.get(face[1]))
                        .fma(1.0F - weightA - weightB, vertices.get(face[2])));
                shards.add(new int[]{face[0], face[1], center});
                shards.add(new int[]{face[1], face[2], center});
                shards.add(new int[]{face[2], face[0], center});
            } else {
                shards.add(face);
            }
        }
        ByteBufferBuilder sourceBuffer = new ByteBufferBuilder(1024);
        BufferBuilder builder = new BufferBuilder(
                sourceBuffer,
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );
        for (int[] face : shards) {
            Vector3f a = vertices.get(face[0]);
            Vector3f b = vertices.get(face[1]);
            Vector3f c = vertices.get(face[2]);
            Vector3f center = new Vector3f(a).add(b).add(c).div(3.0F * radius);
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);
            if (beams) {
                if (green >= 240) {
                    addBeams(builder, b, c, center, red, green, blue);
                }
            } else {
                // UVs carry barycentric coordinates, while the normal carries the shard's pivot
                addVertex(builder, a, center, 1, 0, red, green, blue);
                addVertex(builder, b, center, 0, 1, red, green, blue);
                addVertex(builder, c, center, 0, 0, red, green, blue);
            }
        }
        MeshData mesh = builder.buildOrThrow();
        CirrusVertexBuffer buffer = new CirrusVertexBuffer();
        try {
            buffer.upload(mesh);
        } finally {
            sourceBuffer.close();
        }
        return buffer;
    }

    private static void addBeams(
            BufferBuilder builder, Vector3f edgeStart, Vector3f edgeEnd, Vector3f center,
            int red, int green, int blue
    ) {
        RandomSource random = RandomSource.create((red << 16) | (green << 8) | blue);
        Vector3f origin = new Vector3f(edgeEnd).lerp(edgeStart, 0.25F + red / 510.0F);
        int count = 1 + random.nextInt(2);
        for (int beam = 0; beam < count; beam++) {
            // keep the shard's pivot and seed so each source follows its opening crack
            for (int side = 0; side < 3; side++) {
                addVertex(builder, origin, center, 0, beam * 4, red, green, blue);
                addVertex(builder, origin, center, 1, beam * 4 + side, red, green, blue);
                addVertex(builder, origin, center, 1, beam * 4 + side + 1, red, green, blue);
            }
        }
    }

    private static int midpoint(List<Vector3f> vertices, Map<Long, Integer> midpoints, int first, int second) {
        long key = ((long)Math.min(first, second) << 32) | Math.max(first, second);
        return midpoints.computeIfAbsent(key, ignored -> {
            int index = vertices.size();
            vertices.add(new Vector3f(vertices.get(first)).add(vertices.get(second)).normalize());
            return index;
        });
    }

    private static void addVertex(
            BufferBuilder builder, Vector3f vertex, Vector3f center,
            float u, float v, int red, int green, int blue
    ) {
        builder.addVertex(vertex.x, vertex.y, vertex.z)
                .setUv(u, v)
                .setColor(red, green, blue, 255)
                .setNormal(center.x, center.y, center.z);
    }
}
