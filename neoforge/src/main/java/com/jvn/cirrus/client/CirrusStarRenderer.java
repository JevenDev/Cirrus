package com.jvn.cirrus.client;

import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.config.CirrusConfig;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CirrusStarRenderer implements AutoCloseable {
    public static final int MAX_STAR_COUNT = 8000;
    private static final int SHOOTING_STAR_CANDIDATE_COUNT = 32;
    private static final int SHOOTING_STAR_TRAIL_SEGMENTS = 12;
    private static final float CELESTIAL_RADIUS = 100.0F;
    private static final long STAR_SEED = 10842L;
    private static final long SHOOTING_STAR_SEED = 734287L;
    private static final ResourceLocation NORTH_STAR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Cirrus.MOD_ID, "textures/environment/north_star.png");
    private static final float FULL_ROTATION = (float)(Math.PI * 2.0);
    private static final float[][] CORNERS = {
            {1.0F, -1.0F},
            {1.0F, 1.0F},
            {-1.0F, 1.0F},
            {-1.0F, -1.0F}
    };

    private VertexBuffer starBuffer;
    private VertexBuffer northStarBuffer;
    private VertexBuffer shootingStarBuffer;

    public void render(
            ClientLevel level,
            Matrix4f modelViewMatrix,
            Matrix4f projectionMatrix,
            Matrix4f fixedSkyModelViewMatrix,
            float partialTick,
            int ticks,
            boolean renderStarField
    ) {
        float visibility = level.getStarBrightness(partialTick)
                * (1.0F - level.getRainLevel(partialTick));
        if (visibility <= 0.0F) {
            return;
        }

        float shootingFrequency = CirrusConfig.SHOOTING_STAR_FREQUENCY.get().floatValue();
        boolean renderShootingStars = CirrusConfig.SHOOTING_STARS_ENABLED.get()
                && shootingFrequency > 0.0F
                && CirrusConfig.SHOOTING_STAR_OPACITY.get() > 0.0;
        if (!renderStarField && !renderShootingStars) {
            return;
        }

        prepareStars();
        ShaderInstance shader = CirrusShaders.stars();
        float minimumOpacity = CirrusConfig.STAR_MIN_OPACITY.get().floatValue();
        float maximumOpacity = CirrusConfig.STAR_MAX_OPACITY.get().floatValue();
        float minimumSize = CirrusConfig.STAR_MIN_SIZE.get().floatValue();
        float maximumSize = CirrusConfig.STAR_MAX_SIZE.get().floatValue();
        float twinkleStrength = CirrusConfig.STAR_TWINKLE_STRENGTH.get().floatValue();

        Uniform appearance = shader.getUniform("CirrusStarAppearance");
        if (appearance != null) {
            appearance.set(
                    CirrusConfig.STAR_DENSITY.get() / (float)MAX_STAR_COUNT,
                    Math.min(minimumOpacity, maximumOpacity),
                    Math.max(minimumOpacity, maximumOpacity),
                    twinkleStrength
            );
        }

        Uniform animation = shader.getUniform("CirrusStarAnimation");
        if (animation != null) {
            animation.set(
                    Math.min(minimumSize, maximumSize),
                    Math.max(minimumSize, maximumSize),
                    (ticks + partialTick) / 20.0F,
                    CirrusConfig.STAR_TWINKLE_SPEED.get().floatValue()
            );
        }

        Uniform atmosphere = shader.getUniform("CirrusStarAtmosphere");
        if (atmosphere != null) {
            atmosphere.set(
                    visibility,
                    CirrusConfig.STAR_COLOR_VARIATION.get().floatValue()
            );
        }

        float shootingMinimumSize = CirrusConfig.SHOOTING_STAR_MIN_SIZE.get().floatValue();
        float shootingMaximumSize = CirrusConfig.SHOOTING_STAR_MAX_SIZE.get().floatValue();
        Uniform shootingAppearance = shader.getUniform("CirrusShootingStarAppearance");
        if (shootingAppearance != null) {
            shootingAppearance.set(
                    CirrusConfig.SHOOTING_STARS_ENABLED.get() ? 1.0F : 0.0F,
                    Math.min(shootingMinimumSize, shootingMaximumSize),
                    Math.max(shootingMinimumSize, shootingMaximumSize),
                    CirrusConfig.SHOOTING_STAR_OPACITY.get().floatValue()
            );
        }

        float shootingMinimumSpeed = CirrusConfig.SHOOTING_STAR_MIN_SPEED.get().floatValue();
        float shootingMaximumSpeed = CirrusConfig.SHOOTING_STAR_MAX_SPEED.get().floatValue();
        Uniform shootingAnimation = shader.getUniform("CirrusShootingStarAnimation");
        if (shootingAnimation != null) {
            shootingAnimation.set(
                    (ticks + partialTick) / 20.0F,
                    shootingFrequency,
                    Math.min(shootingMinimumSpeed, shootingMaximumSpeed),
                    Math.max(shootingMinimumSpeed, shootingMaximumSpeed)
            );
        }

        Uniform shootingVisual = shader.getUniform("CirrusShootingStarVisual");
        if (shootingVisual != null) {
            shootingVisual.set(
                    CirrusConfig.SHOOTING_STAR_TRAIL_LENGTH.get().floatValue(),
                    CirrusConfig.SHOOTING_STAR_BLOOM.get().floatValue(),
                    CirrusConfig.SHOOTING_STAR_COLOR_VARIATION.get().floatValue(),
                    0.0F
            );
        }

        Uniform renderMode = shader.getUniform("CirrusStarRenderMode");
        if (renderMode != null) {
            renderMode.set(0.0F);
        }

        float[] previousColor = RenderSystem.getShaderColor();
        int previousTexture = RenderSystem.getShaderTexture(0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, NORTH_STAR_TEXTURE);
        try {
            if (renderStarField) {
                starBuffer.bind();
                starBuffer.drawWithShader(modelViewMatrix, projectionMatrix, shader);
            }

            if (renderShootingStars) {
                if (renderMode != null) {
                    renderMode.set(2.0F);
                }
                shootingStarBuffer.bind();
                shootingStarBuffer.drawWithShader(fixedSkyModelViewMatrix, projectionMatrix, shader);
            }

            // Polaris is world-fixed instead of following the rotating
            // celestial matrix. Keep it bright, large, and gently twinkling.
            if (renderStarField) {
                if (appearance != null) {
                    appearance.set(
                            1.0F,
                            Math.max(minimumOpacity, maximumOpacity),
                            Math.max(minimumOpacity, maximumOpacity),
                            0.0F
                    );
                }
                if (renderMode != null) {
                    renderMode.set(1.0F);
                }
                northStarBuffer.bind();
                northStarBuffer.drawWithShader(fixedSkyModelViewMatrix, projectionMatrix, shader);
            }
        } finally {
            VertexBuffer.unbind();
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShaderColor(
                    previousColor[0],
                    previousColor[1],
                    previousColor[2],
                    previousColor[3]
            );
        }
    }

    private void prepareStars() {
        if (starBuffer != null) {
            return;
        }

        Random random = new Random(STAR_SEED);
        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );
        for (int index = 0; index < MAX_STAR_COUNT; index++) {
            float vertical = random.nextFloat() * 2.0F - 1.0F;
            float azimuth = random.nextFloat() * FULL_ROTATION;
            float horizontal = Mth.sqrt(1.0F - vertical * vertical);
            Vector3f direction = new Vector3f(
                    horizontal * Mth.cos(azimuth),
                    vertical,
                    horizontal * Mth.sin(azimuth)
            );
            Vector3f tangent = Math.abs(vertical) < 0.999F
                    ? new Vector3f(-direction.z, 0.0F, direction.x).normalize()
                    : new Vector3f(1.0F, 0.0F, 0.0F);
            Vector3f bitangent = new Vector3f(tangent).cross(direction);
            float rotation = random.nextFloat() * FULL_ROTATION;
            tangent.mul(Mth.cos(rotation)).add(bitangent.mul(Mth.sin(rotation))).normalize();

            float opacityRandom = random.nextFloat();
            float sizeRandom = random.nextFloat();
            float colorRandom = random.nextFloat();
            float twinklePhase = random.nextFloat();
            for (float[] corner : CORNERS) {
                builder.addVertex(
                                direction.x * CELESTIAL_RADIUS,
                                direction.y * CELESTIAL_RADIUS,
                                direction.z * CELESTIAL_RADIUS
                        )
                        .setUv(corner[0], corner[1])
                        .setColor(opacityRandom, sizeRandom, colorRandom, twinklePhase)
                        .setNormal(tangent.x, tangent.y, tangent.z);
            }
        }

        MeshData mesh = builder.buildOrThrow();
        starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        starBuffer.bind();
        starBuffer.upload(mesh);
        VertexBuffer.unbind();

        Random shootingRandom = new Random(SHOOTING_STAR_SEED);
        BufferBuilder shootingBuilder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );
        for (int index = 0; index < SHOOTING_STAR_CANDIDATE_COUNT; index++) {
            float vertical = 0.25F + shootingRandom.nextFloat() * 0.70F;
            float azimuth = shootingRandom.nextFloat() * FULL_ROTATION;
            float horizontal = Mth.sqrt(1.0F - vertical * vertical);
            Vector3f direction = new Vector3f(
                    horizontal * Mth.cos(azimuth),
                    vertical,
                    horizontal * Mth.sin(azimuth)
            );
            Vector3f tangent = new Vector3f(-direction.z, 0.0F, direction.x).normalize();
            Vector3f bitangent = new Vector3f(tangent).cross(direction);
            float rotation = shootingRandom.nextFloat() * FULL_ROTATION;
            tangent.mul(Mth.cos(rotation)).add(bitangent.mul(Mth.sin(rotation))).normalize();

            float sizeRandom = shootingRandom.nextFloat();
            float speedRandom = shootingRandom.nextFloat();
            float colorRandom = shootingRandom.nextFloat();
            float candidateSlot = index / (float)(SHOOTING_STAR_CANDIDATE_COUNT - 1);
            for (int segment = 0; segment < SHOOTING_STAR_TRAIL_SEGMENTS; segment++) {
                float segmentStart = segment / (float)SHOOTING_STAR_TRAIL_SEGMENTS;
                float segmentEnd = (segment + 1) / (float)SHOOTING_STAR_TRAIL_SEGMENTS;
                for (float[] corner : CORNERS) {
                    float alongTrail = corner[0] > 0.0F ? segmentEnd : segmentStart;
                    shootingBuilder.addVertex(
                                    direction.x * CELESTIAL_RADIUS,
                                    direction.y * CELESTIAL_RADIUS,
                                    direction.z * CELESTIAL_RADIUS
                            )
                            .setUv(alongTrail, corner[1])
                            .setColor(sizeRandom, speedRandom, colorRandom, candidateSlot)
                            .setNormal(tangent.x, tangent.y, tangent.z);
                }
            }
        }

        shootingStarBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        shootingStarBuffer.bind();
        shootingStarBuffer.upload(shootingBuilder.buildOrThrow());
        VertexBuffer.unbind();

        float northElevation = (float)Math.toRadians(45.0);
        Vector3f northDirection = new Vector3f(
                0.0F,
                Mth.sin(northElevation),
                -Mth.cos(northElevation)
        );
        BufferBuilder northBuilder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL
        );
        for (float[] corner : CORNERS) {
            northBuilder.addVertex(
                            northDirection.x * CELESTIAL_RADIUS,
                            northDirection.y * CELESTIAL_RADIUS,
                            northDirection.z * CELESTIAL_RADIUS
                    )
                    .setUv(corner[0], corner[1])
                    .setColor(1.0F, 1.0F, 0.68F, 0.15F)
                    .setNormal(1.0F, 0.0F, 0.0F);
        }

        northStarBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        northStarBuffer.bind();
        northStarBuffer.upload(northBuilder.buildOrThrow());
        VertexBuffer.unbind();
    }

    @Override
    public void close() {
        if (starBuffer != null) {
            starBuffer.close();
            starBuffer = null;
        }
        if (northStarBuffer != null) {
            northStarBuffer.close();
            northStarBuffer = null;
        }
        if (shootingStarBuffer != null) {
            shootingStarBuffer.close();
            shootingStarBuffer = null;
        }
    }
}
