package com.jvn.cirrus.client.compat.distanthorizons;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeApplyShaderRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.function.Consumer;

final class DistantHorizonsApiCompat {
    private static final float[] DH_PROJECTION_MATRIX_VALUES = new float[16];
    private static IDhApiConfig configs;
    private static IDhApiConfigValue<Boolean> cloudRendering;
    private static IDhApiConfigValue<Integer> chunkRenderDistance;
    private static Boolean lastCloudOverrideRequest;
    private static boolean overrideApplied;
    private static boolean beforeApplyShaderEventRegistered;
    private static Consumer<float[]> beforeApplyShaderCallback;
    private static ExternalFramebufferTarget externalFramebufferTarget;
    private static final DhApiBeforeApplyShaderRenderEvent BEFORE_APPLY_SHADER_EVENT =
            new DhApiBeforeApplyShaderRenderEvent() {
        @Override
        public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
            Consumer<float[]> callback = beforeApplyShaderCallback;
            if (callback != null) {
                event.value.dhProjectionMatrix.putValuesInArray(DH_PROJECTION_MATRIX_VALUES);
                int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
                int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
                GpuTextureView previousColorOverride = RenderSystem.outputColorTextureOverride;
                GpuTextureView previousDepthOverride = RenderSystem.outputDepthTextureOverride;
                try {
                    ExternalFramebufferTarget target = externalFramebufferTarget(drawFramebuffer);
                    if (target == null) {
                        // Let Minecraft's normal cloud pass handle unsupported DH targets.
                        return;
                    }
                    RenderSystem.outputColorTextureOverride = target.colorView;
                    RenderSystem.outputDepthTextureOverride = target.depthView;
                    callback.accept(DH_PROJECTION_MATRIX_VALUES);
                } finally {
                    RenderSystem.outputColorTextureOverride = previousColorOverride;
                    RenderSystem.outputDepthTextureOverride = previousDepthOverride;
                    // Minecraft render passes bind framebuffer 0 when they close.
                    // DH's apply shader expects its framebuffer to remain bound while it
                    // temporarily attaches Minecraft's color texture for the composite.
                    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
                }
            }
        }
    };

    private DistantHorizonsApiCompat() {
    }

    static int beginFrame(
            boolean disableDistantClouds,
            boolean syncCloudDistance,
            int fallbackDistance
    ) {
        registerBeforeApplyShaderEvent();
        refreshConfigHandles();
        updateCloudOverride(disableDistantClouds);
        if (!syncCloudDistance || chunkRenderDistance == null) {
            return fallbackDistance;
        }

        Integer distance = chunkRenderDistance.getValue();
        return distance != null && distance > 0 ? distance : fallbackDistance;
    }

    static void setBeforeApplyShaderCallback(Consumer<float[]> callback) {
        beforeApplyShaderCallback = callback;
    }

    private static void refreshConfigHandles() {
        IDhApiConfig currentConfigs = DhApi.Delayed.configs;
        if (currentConfigs == configs) {
            return;
        }

        if (overrideApplied && cloudRendering != null) {
            cloudRendering.clearValue();
        }
        configs = currentConfigs;
        cloudRendering = null;
        chunkRenderDistance = null;
        lastCloudOverrideRequest = null;
        overrideApplied = false;

        if (configs != null) {
            cloudRendering = configs.graphics().genericRendering().cloudRenderingEnabled();
            chunkRenderDistance = configs.graphics().chunkRenderDistance();
        }
    }

    private static void updateCloudOverride(boolean disableDistantClouds) {
        if (cloudRendering == null
                || Boolean.valueOf(disableDistantClouds).equals(lastCloudOverrideRequest)) {
            return;
        }

        if (overrideApplied) {
            cloudRendering.clearValue();
            overrideApplied = false;
        }

        if (disableDistantClouds && !Boolean.FALSE.equals(cloudRendering.getApiValue())) {
            overrideApplied = cloudRendering.setValue(false);
        }
        lastCloudOverrideRequest = disableDistantClouds;
    }

    private static void registerBeforeApplyShaderEvent() {
        if (!beforeApplyShaderEventRegistered) {
            beforeApplyShaderEventRegistered = DhApiEventRegister.on(
                    DhApiBeforeApplyShaderRenderEvent.class,
                    BEFORE_APPLY_SHADER_EVENT
            ).success;
        }
    }

    private static ExternalFramebufferTarget externalFramebufferTarget(int framebuffer) {
        if (framebuffer == 0) {
            return null;
        }

        int colorTexture = attachmentTexture(GL30.GL_COLOR_ATTACHMENT0);
        int depthTexture = attachmentTexture(GL30.GL_DEPTH_ATTACHMENT);
        if (colorTexture == 0 || depthTexture == 0) {
            return null;
        }

        GpuTextureView mainColor = Minecraft.getInstance()
                .getMainRenderTarget()
                .getColorTextureView();
        int width = mainColor.getWidth(0);
        int height = mainColor.getHeight(0);
        if (externalFramebufferTarget == null
                || !externalFramebufferTarget.matches(
                        framebuffer, colorTexture, depthTexture, width, height
                )) {
            externalFramebufferTarget = new ExternalFramebufferTarget(
                    framebuffer, colorTexture, depthTexture, width, height
            );
        }
        return externalFramebufferTarget;
    }

    private static int attachmentTexture(int attachment) {
        int type = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_DRAW_FRAMEBUFFER,
                attachment,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        );
        if (type != GL11.GL_TEXTURE) {
            return 0;
        }
        return GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_DRAW_FRAMEBUFFER,
                attachment,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
        );
    }

    private static final class ExternalFramebufferTarget {
        private final int framebuffer;
        private final int colorTexture;
        private final int depthTexture;
        private final int width;
        private final int height;
        private final GpuTextureView colorView;
        private final GpuTextureView depthView;

        private ExternalFramebufferTarget(
                int framebuffer,
                int colorTexture,
                int depthTexture,
                int width,
                int height
        ) {
            this.framebuffer = framebuffer;
            this.colorTexture = colorTexture;
            this.depthTexture = depthTexture;
            this.width = width;
            this.height = height;
            this.colorView = new ExternalTextureView(
                    new ExternalTexture("Distant Horizons color", TextureFormat.RGBA8,
                            width, height, colorTexture, framebuffer)
            );
            this.depthView = new ExternalTextureView(
                    new ExternalTexture("Distant Horizons depth", TextureFormat.DEPTH32,
                            width, height, depthTexture, framebuffer)
            );
        }

        private boolean matches(
                int framebuffer,
                int colorTexture,
                int depthTexture,
                int width,
                int height
        ) {
            return this.framebuffer == framebuffer
                    && this.colorTexture == colorTexture
                    && this.depthTexture == depthTexture
                    && this.width == width
                    && this.height == height;
        }
    }

    private static final class ExternalTexture extends GlTexture {
        private final int framebuffer;

        private ExternalTexture(
                String label,
                TextureFormat format,
                int width,
                int height,
                int id,
                int framebuffer
        ) {
            super(GpuTexture.USAGE_RENDER_ATTACHMENT, label, format, width, height, 1, 1, id);
            this.framebuffer = framebuffer;
        }

        @Override
        public void close() {
            // DH owns the OpenGL texture.
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public int getFbo(DirectStateAccess directStateAccess, GpuTexture depthTexture) {
            return framebuffer;
        }
    }

    private static final class ExternalTextureView extends GlTextureView {
        private ExternalTextureView(ExternalTexture texture) {
            super(texture, 0, 1);
        }

        @Override
        public void close() {
            // DH owns the framebuffer and its attachments.
        }

        @Override
        public boolean isClosed() {
            return false;
        }
    }
}
