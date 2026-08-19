package com.jvn.cirrus.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.jvn.cirrus.client.render.CirrusVertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.Mth;

public final class CirrusSkyDome {
    private CirrusSkyDome() {
    }

    public static CirrusVertexBuffer create(
            float radius,
            int azimuthSegments,
            int elevationSegments,
            float minimumElevation,
            float maximumElevation
    ) {
        validate(radius, azimuthSegments, elevationSegments, minimumElevation, maximumElevation);

        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION
        );
        for (int elevationIndex = 0; elevationIndex < elevationSegments; elevationIndex++) {
            float elevation0 = Mth.lerp(
                    elevationIndex / (float) elevationSegments,
                    minimumElevation,
                    maximumElevation
            );
            float elevation1 = Mth.lerp(
                    (elevationIndex + 1) / (float) elevationSegments,
                    minimumElevation,
                    maximumElevation
            );
            for (int azimuthIndex = 0; azimuthIndex < azimuthSegments; azimuthIndex++) {
                float azimuth0 = azimuthIndex * Mth.TWO_PI / azimuthSegments;
                float azimuth1 = (azimuthIndex + 1) * Mth.TWO_PI / azimuthSegments;
                addVertex(builder, radius, azimuth0, elevation0);
                addVertex(builder, radius, azimuth0, elevation1);
                addVertex(builder, radius, azimuth1, elevation1);
                addVertex(builder, radius, azimuth1, elevation0);
            }
        }

        MeshData mesh = builder.buildOrThrow();
        CirrusVertexBuffer buffer = new CirrusVertexBuffer();
        buffer.bind();
        try {
            buffer.upload(mesh);
        } catch (RuntimeException exception) {
            buffer.close();
            throw exception;
        } finally {
            CirrusVertexBuffer.unbind();
        }
        return buffer;
    }

    private static void validate(
            float radius,
            int azimuthSegments,
            int elevationSegments,
            float minimumElevation,
            float maximumElevation
    ) {
        if (!Float.isFinite(radius) || radius <= 0.0F) {
            throw new IllegalArgumentException("radius must be finite and positive");
        }
        if (azimuthSegments <= 0 || elevationSegments <= 0) {
            throw new IllegalArgumentException("segment counts must be positive");
        }
        if (!Float.isFinite(minimumElevation)
                || !Float.isFinite(maximumElevation)
                || maximumElevation <= minimumElevation) {
            throw new IllegalArgumentException("elevation bounds must be finite and increasing");
        }
    }

    private static void addVertex(BufferBuilder builder, float radius, float azimuth, float elevation) {
        float horizontal = Mth.cos(elevation) * radius;
        builder.addVertex(
                Mth.sin(azimuth) * horizontal,
                Mth.sin(elevation) * radius,
                Mth.cos(azimuth) * horizontal
        );
    }
}
