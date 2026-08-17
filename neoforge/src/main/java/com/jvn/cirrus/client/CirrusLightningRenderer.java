package com.jvn.cirrus.client;

import static com.jvn.toucanlib.util.ToucanRandom.signedDouble;

import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class CirrusLightningRenderer {
    private static final int TRUNK_STEPS = 24;
    private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 WORLD_EAST = new Vec3(1.0, 0.0, 0.0);
    private static final Map<LightningBolt, CachedGeometry> GEOMETRY_CACHE = new WeakHashMap<>();
    private static final RenderType GLOW_RENDER_TYPE = RenderType.create(
            "cirrus_lightning_glow",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setOutputState(RenderStateShard.WEATHER_TARGET)
                    .createCompositeState(false)
    );
    private static final RenderType CORE_RENDER_TYPE = RenderType.create(
            "cirrus_lightning_core",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .setOutputState(RenderStateShard.WEATHER_TARGET)
                    .createCompositeState(false)
    );

    private CirrusLightningRenderer() {
    }

    public static void render(
            LightningBolt lightning,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers
    ) {
        Vec3 visualOrigin = CirrusCloudAttachment.findVisualOrigin(lightning, partialTick);
        CachedGeometry geometry = GEOMETRY_CACHE.get(lightning);
        if (geometry == null || !geometry.visualOrigin().equals(visualOrigin)) {
            RandomSource random = RandomSource.create(lightning.seed);
            List<Segment> builtSegments = new ArrayList<>(128);
            Vec3[] trunk = buildTrunk(random, builtSegments, visualOrigin);
            buildBranches(random, trunk, builtSegments);
            geometry = new CachedGeometry(visualOrigin, List.copyOf(builtSegments));
            GEOMETRY_CACHE.put(lightning, geometry);
        }
        List<Segment> segments = geometry.segments();

        float configuredIntensity = CirrusConfig.LIGHTNING_BOLT_INTENSITY.get().floatValue();
        float age = lightning.tickCount + partialTick;
        float seedPhase = (float)((lightning.seed & 1023L) * (Mth.TWO_PI / 1024.0));
        float pulse = 0.84F + 0.16F * Mth.cos(age * 11.0F + seedPhase);
        float intensity = configuredIntensity * pulse;

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer glowConsumer = buffers.getBuffer(GLOW_RENDER_TYPE);
        drawLayer(pose, glowConsumer, segments, 4.8F, 0.22F, 0.32F, 1.00F,
                0.055F * intensity, false);
        drawLayer(pose, glowConsumer, segments, 2.15F, 0.48F, 0.68F, 1.00F,
                0.13F * intensity, false);

        VertexConsumer coreConsumer = buffers.getBuffer(CORE_RENDER_TYPE);
        drawLayer(pose, coreConsumer, segments, 1.0F, 0.88F, 0.94F, 1.00F,
                0.72F * intensity, true);
    }

    private static Vec3[] buildTrunk(
            RandomSource random,
            List<Segment> segments,
            Vec3 visualOrigin
    ) {
        Vec3[] points = new Vec3[TRUNK_STEPS + 1];
        float[] widths = new float[TRUNK_STEPS + 1];
        float[] brightness = new float[TRUNK_STEPS + 1];
        points[0] = Vec3.ZERO;
        widths[0] = 0.105F;
        brightness[0] = 1.0F;
        double x = 0.0;
        double z = 0.0;
        double boltHeight = Math.max(16.0, visualOrigin.y);
        double stepHeight = boltHeight / TRUNK_STEPS;

        for (int step = 1; step <= TRUNK_STEPS; step++) {
            double heightProgress = step / (double)TRUNK_STEPS;
            double wander = Mth.lerp(heightProgress, 0.72, 2.15);
            x += signedDouble(random) * wander;
            z += signedDouble(random) * wander;

            if (step > 2 && step < TRUNK_STEPS - 1 && random.nextFloat() < 0.22F) {
                x += signedDouble(random) * 2.4;
                z += signedDouble(random) * 2.4;
            }

            points[step] = new Vec3(x, step * stepHeight, z);
            widths[step] = Mth.lerp((float)heightProgress, 0.105F, 0.22F);
            brightness[step] = 1.0F;
        }

        double correctionX = visualOrigin.x - points[TRUNK_STEPS].x;
        double correctionZ = visualOrigin.z - points[TRUNK_STEPS].z;
        for (int step = 1; step <= TRUNK_STEPS; step++) {
            double progress = step / (double)TRUNK_STEPS;
            double attachmentBlend = progress * progress * (3.0 - 2.0 * progress);
            Vec3 point = points[step];
            points[step] = new Vec3(
                    point.x + correctionX * attachmentBlend,
                    point.y,
                    point.z + correctionZ * attachmentBlend
            );
        }

        appendPath(points, widths, brightness, segments);
        return points;
    }

    private static void buildBranches(RandomSource random, Vec3[] trunk, List<Segment> segments) {
        int nextAllowedStep = 4;
        for (int step = 5; step < TRUNK_STEPS; step++) {
            if (step < nextAllowedStep || random.nextFloat() > 0.48F) {
                continue;
            }

            int length = 3 + random.nextInt(5);
            double angle = random.nextDouble() * Mth.TWO_PI;
            double horizontalSpeed = 1.3 + random.nextDouble() * 1.8;
            Vec3 direction = new Vec3(Math.cos(angle) * horizontalSpeed, 0.0,
                    Math.sin(angle) * horizontalSpeed);
            buildBranch(random, trunk[step], direction, length,
                    0.092F + random.nextFloat() * 0.035F, 0.72F + random.nextFloat() * 0.24F,
                    segments, true);
            nextAllowedStep = step + 2 + random.nextInt(3);
        }
    }

    private static void buildBranch(
            RandomSource random,
            Vec3 origin,
            Vec3 initialDirection,
            int length,
            float startWidth,
            float brightness,
            List<Segment> segments,
            boolean allowFork
    ) {
        Vec3[] points = new Vec3[length + 1];
        float[] widths = new float[length + 1];
        float[] brightnessValues = new float[length + 1];
        Vec3 point = origin;
        Vec3 direction = initialDirection;
        double drop = 2.7 + random.nextDouble() * 2.2;
        points[0] = origin;
        widths[0] = startWidth;
        brightnessValues[0] = brightness;

        for (int index = 0; index < length; index++) {
            float progress = index / (float)length;
            direction = direction.add(signedDouble(random) * 0.85, 0.0, signedDouble(random) * 0.85);
            Vec3 next = point.add(direction.x, -drop, direction.z);
            float fromWidth = Mth.lerp(progress, startWidth, 0.018F);
            float toWidth = Mth.lerp((index + 1.0F) / length, startWidth, 0.018F);
            points[index] = point;
            widths[index] = fromWidth;
            brightnessValues[index] = brightness * Mth.lerp(progress, 1.0F, 0.28F);
            points[index + 1] = next;
            widths[index + 1] = toWidth;
            brightnessValues[index + 1] = brightness
                    * Mth.lerp((index + 1.0F) / length, 1.0F, 0.28F);

            if (allowFork && index >= 1 && index < length - 2 && random.nextFloat() < 0.28F) {
                double forkAngle = (random.nextBoolean() ? 1.0 : -1.0)
                        * (0.55 + random.nextDouble() * 0.7);
                double cos = Math.cos(forkAngle);
                double sin = Math.sin(forkAngle);
                Vec3 forkDirection = new Vec3(
                        direction.x * cos - direction.z * sin,
                        0.0,
                        direction.x * sin + direction.z * cos
                ).scale(0.72);
                buildBranch(random, next, forkDirection, 2 + random.nextInt(3),
                        toWidth * 0.72F, brightness * 0.58F, segments, false);
            }
            point = next;
        }

        appendPath(points, widths, brightnessValues, segments);
    }

    private static void appendPath(
            Vec3[] points,
            float[] widths,
            float[] brightness,
            List<Segment> segments
    ) {
        for (int index = 0; index < points.length - 1; index++) {
            segments.add(new Segment(
                    points[index],
                    points[index + 1],
                    tangentAt(points, index),
                    tangentAt(points, index + 1),
                    widths[index],
                    widths[index + 1],
                    brightness[index],
                    brightness[index + 1],
                    index == 0
            ));
        }
    }

    private static Vec3 tangentAt(Vec3[] points, int index) {
        if (index == 0) {
            return points[1].subtract(points[0]).normalize();
        }
        if (index == points.length - 1) {
            return points[index].subtract(points[index - 1]).normalize();
        }
        return points[index + 1].subtract(points[index - 1]).normalize();
    }

    private static void drawLayer(
            Matrix4f pose,
            VertexConsumer consumer,
            List<Segment> segments,
            float widthScale,
            float red,
            float green,
            float blue,
            float alpha,
            boolean hotCore
    ) {
        float clampedAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
        Vec3 previousSideA = null;
        Vec3 previousSideB = null;

        for (Segment segment : segments) {
            Vec3 fromSideA;
            Vec3 fromSideB;
            if (segment.pathStart() || previousSideA == null || previousSideB == null) {
                fromSideA = initialSide(segment.fromTangent());
                fromSideB = segment.fromTangent().cross(fromSideA).normalize();
            } else {
                fromSideA = previousSideA;
                fromSideB = previousSideB;
            }

            Vec3 toSideA = transportSide(fromSideA, segment.toTangent());
            Vec3 toSideB = segment.toTangent().cross(toSideA).normalize();
            if (toSideB.dot(fromSideB) < 0.0) {
                toSideB = toSideB.scale(-1.0);
            }

            float branchAlpha = clampedAlpha * segment.fromBrightness();
            float branchTint = hotCore
                    ? Mth.lerp(segment.fromBrightness(), 0.76F, 1.0F)
                    : 1.0F;
            drawCrossedRibbon(
                    pose,
                    consumer,
                    segment,
                    fromSideA,
                    fromSideB,
                    toSideA,
                    toSideB,
                    widthScale,
                    red * branchTint,
                    green * branchTint,
                    blue,
                    Mth.clamp(branchAlpha, 0.0F, 1.0F)
            );
            previousSideA = toSideA;
            previousSideB = toSideB;
        }
    }

    private static Vec3 initialSide(Vec3 tangent) {
        Vec3 reference = Math.abs(tangent.y) > 0.90 ? WORLD_EAST : WORLD_UP;
        return tangent.cross(reference).normalize();
    }

    private static Vec3 transportSide(Vec3 previousSide, Vec3 tangent) {
        Vec3 projected = previousSide.subtract(tangent.scale(previousSide.dot(tangent)));
        if (projected.lengthSqr() < 1.0E-7) {
            return initialSide(tangent);
        }
        Vec3 transported = projected.normalize();
        return transported.dot(previousSide) < 0.0 ? transported.scale(-1.0) : transported;
    }

    private static void drawCrossedRibbon(
            Matrix4f pose,
            VertexConsumer consumer,
            Segment segment,
            Vec3 fromSideA,
            Vec3 fromSideB,
            Vec3 toSideA,
            Vec3 toSideB,
            float widthScale,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        ribbon(pose, consumer, segment, fromSideA, toSideA, widthScale,
                red, green, blue, alpha);
        ribbon(pose, consumer, segment, fromSideB, toSideB, widthScale,
                red, green, blue, alpha);
    }

    private static void ribbon(
            Matrix4f pose,
            VertexConsumer consumer,
            Segment segment,
            Vec3 fromSide,
            Vec3 toSide,
            float widthScale,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3 fromOffset = fromSide.scale(segment.fromWidth() * widthScale);
        Vec3 toOffset = toSide.scale(segment.toWidth() * widthScale);
        vertex(pose, consumer, segment.from().add(fromOffset), red, green, blue, alpha);
        vertex(pose, consumer, segment.to().add(toOffset), red, green, blue, alpha);
        vertex(pose, consumer, segment.to().subtract(toOffset), red, green, blue, alpha);
        vertex(pose, consumer, segment.from().subtract(fromOffset), red, green, blue, alpha);
    }

    private static void vertex(
            Matrix4f pose,
            VertexConsumer consumer,
            Vec3 position,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        consumer.addVertex(pose, (float)position.x, (float)position.y, (float)position.z)
                .setColor(red, green, blue, alpha);
    }

    private record Segment(
            Vec3 from,
            Vec3 to,
            Vec3 fromTangent,
            Vec3 toTangent,
            float fromWidth,
            float toWidth,
            float fromBrightness,
            float toBrightness,
            boolean pathStart
    ) {
    }

    private record CachedGeometry(Vec3 visualOrigin, List<Segment> segments) {
    }

}
