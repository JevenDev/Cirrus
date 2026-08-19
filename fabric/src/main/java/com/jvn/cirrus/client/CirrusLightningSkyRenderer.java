package com.jvn.cirrus.client;

import com.jvn.cirrus.client.util.CirrusSkyDome;
import com.jvn.cirrus.client.util.CirrusShaderUniforms;
import com.mojang.blaze3d.vertex.PoseStack;
import com.jvn.cirrus.client.render.CirrusVertexBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.jvn.cirrus.client.render.CirrusShader;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CirrusLightningSkyRenderer implements AutoCloseable {
    private static final float DOME_RADIUS = 100.0F;
    private static final int AZIMUTH_SEGMENTS = 96;
    private static final int ELEVATION_SEGMENTS = 32;
    private static final float MIN_ELEVATION = (float)Math.toRadians(-18.0);
    private static final float MAX_ELEVATION = (float)Math.toRadians(90.0);

    private CirrusVertexBuffer domeBuffer;

    public void render(
            ClientLevel level,
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera
    ) {
        if (Minecraft.getInstance().options.hideLightningFlash().get()) {
            return;
        }

        LightningBolt lightning = CirrusLightningLocator.nearest(level, camera.position());
        if (lightning == null) {
            return;
        }
        float intensity = Mth.clamp(1.0F - (lightning.tickCount + partialTick) / 3.0F, 0.0F, 1.0F);
        if (intensity < 0.002F) {
            return;
        }

        Vec3 cameraPosition = camera.position();
        Vec3 visualOrigin = CirrusCloudAttachment.findVisualOrigin(lightning, partialTick);
        Vec3 flashDirection = new Vec3(
                lightning.getX() + visualOrigin.x - cameraPosition.x,
                lightning.getY() + visualOrigin.y - cameraPosition.y,
                lightning.getZ() + visualOrigin.z - cameraPosition.z
        ).normalize();

        prepareDome();
        CirrusShader shader = CirrusShaders.lightningSky();
        CirrusShaderUniforms.setUniform(
                shader, "CirrusLightningSkyDirection",
                (float)flashDirection.x, (float)flashDirection.y, (float)flashDirection.z
        );
        CirrusShaderUniforms.setUniform(shader, "CirrusLightningSkyIntensity", intensity);

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(frustumMatrix);
        domeBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
    }

    private void prepareDome() {
        if (domeBuffer == null) {
            domeBuffer = CirrusSkyDome.create(
                    DOME_RADIUS, AZIMUTH_SEGMENTS, ELEVATION_SEGMENTS, MIN_ELEVATION, MAX_ELEVATION
            );
        }
    }

    @Override
    public void close() {
        if (domeBuffer != null) {
            domeBuffer.close();
            domeBuffer = null;
        }
    }
}
