package com.jvn.cirrus.client.compat.distanthorizons;

import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.Optional;
import java.util.OptionalDouble;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiRenderingEngine;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiBlazeTextureWrapper;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderProxy;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeApplyShaderRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.function.BiConsumer;

final class DistantHorizonsApiCompat {
    private static final float[] DH_PROJECTION_MATRIX_VALUES = new float[16];
    private static final float[] DH_MODEL_VIEW_MATRIX_VALUES = new float[16];
    private static IDhApiConfig configs;
    private static IDhApiConfigValue<Boolean> cloudRendering;
    private static IDhApiConfigValue<Integer> chunkRenderDistance;
    private static Boolean lastCloudOverrideRequest;
    private static boolean overrideApplied;
    private static boolean beforeApplyShaderEventRegistered;
    private static BiConsumer<float[], float[]> beforeApplyShaderCallback;
    private static final DhApiBeforeApplyShaderRenderEvent BEFORE_APPLY_SHADER_EVENT =
            new DhApiBeforeApplyShaderRenderEvent() {
        @Override
        public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
            BiConsumer<float[], float[]> callback = beforeApplyShaderCallback;
            if (callback == null) {
                return;
            }

            ExternalFramebufferTarget target = blazeFramebufferTarget();
            if (target == null) {
                return;
            }
            event.value.dhProjectionMatrix.putValuesInArray(DH_PROJECTION_MATRIX_VALUES);
            event.value.dhModelViewMatrix.putValuesInArray(DH_MODEL_VIEW_MATRIX_VALUES);
            boolean openGl = "OpenGL".equals(RenderSystem.getDevice().getDeviceInfo().backendName());
            int drawFramebuffer = openGl ? GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) : 0;
            int readFramebuffer = openGl ? GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING) : 0;
            try {
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "Cirrus Distant Horizons clouds", target.colorView(), Optional.empty(),
                        target.depthView(), OptionalDouble.empty()
                )) {
                    CirrusRenderContext.renderInPass(pass, null,
                            () -> callback.accept(DH_PROJECTION_MATRIX_VALUES, DH_MODEL_VIEW_MATRIX_VALUES));
                }
            } finally {
                if (openGl) {
                    // closing the cloud pass can change the framebuffer DH expects for compositing
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

    static void setBeforeApplyShaderCallback(BiConsumer<float[], float[]> callback) {
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

    private static ExternalFramebufferTarget blazeFramebufferTarget() {
        IDhApiRenderProxy renderProxy = DhApi.Delayed.renderProxy;
        if (renderProxy == null || renderProxy.getRenderingEngine() != EDhApiRenderingEngine.BLAZE_3D) {
            return null;
        }
        GpuTextureView colorView = blazeTextureView(renderProxy.getDhColorTextureBlazeWrapper());
        GpuTextureView depthView = blazeTextureView(renderProxy.getDhDepthTextureBlazeWrapper());
        if (colorView == null || depthView == null) {
            return null;
        }
        return new ExternalFramebufferTarget(colorView, depthView);
    }

    private static GpuTextureView blazeTextureView(DhApiResult<IDhApiBlazeTextureWrapper> result) {
        if (!result.success || result.payload == null) {
            return null;
        }

        // DH exposes the texture, view and sampler in that order, and owns their lifetime
        Object wrapped = result.payload.getWrappedMcObject();
        if (!(wrapped instanceof Object[] objects) || objects.length < 2
                || !(objects[1] instanceof GpuTextureView view) || view.isClosed()) {
            return null;
        }
        return view;
    }

    private record ExternalFramebufferTarget(
            GpuTextureView colorView,
            GpuTextureView depthView
    ) {
    }
}
