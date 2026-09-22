package com.jvn.cirrus.client.compat.distanthorizons;

import com.jvn.cirrus.Cirrus;
import com.jvn.cirrus.client.render.CirrusRenderContext;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.util.Optional;
import java.util.OptionalDouble;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeApplyShaderRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    private static boolean blazeRendererLookupFailed;
    private static Object blazeMetaRenderer;
    private static Field blazeColorWrapperField;
    private static Field blazeDepthWrapperField;
    private static Method blazeTextureViewMethod;
    private static final DhApiBeforeApplyShaderRenderEvent BEFORE_APPLY_SHADER_EVENT =
            new DhApiBeforeApplyShaderRenderEvent() {
        @Override
        public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
            BiConsumer<float[], float[]> callback = beforeApplyShaderCallback;
            if (callback == null || !"OpenGL".equals(RenderSystem.getDevice().getDeviceInfo().backendName())) {
                return;
            }

            event.value.dhProjectionMatrix.putValuesInArray(DH_PROJECTION_MATRIX_VALUES);
            event.value.dhModelViewMatrix.putValuesInArray(DH_MODEL_VIEW_MATRIX_VALUES);
            int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            try {
                ExternalFramebufferTarget target = blazeFramebufferTarget();
                if (target == null) {
                    // Let Minecraft's normal cloud pass handle unsupported DH targets.
                    return;
                }
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> "Cirrus Distant Horizons clouds", target.colorView(), Optional.empty(),
                        target.depthView(), OptionalDouble.empty()
                )) {
                    CirrusRenderContext.renderInPass(pass, null,
                            () -> callback.accept(DH_PROJECTION_MATRIX_VALUES, DH_MODEL_VIEW_MATRIX_VALUES));
                }
            } finally {
                // Render passes can change framebuffer bindings when they close; DH's
                // apply shader expects its framebuffer to remain bound for the composite.
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
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
        if (blazeRendererLookupFailed) {
            return null;
        }

        try {
            if (blazeMetaRenderer == null) {
                Class<?> metaRendererClass = Class.forName(
                        "com.seibel.distanthorizons.common.render.blaze.BlazeDhMetaRenderer"
                );
                blazeMetaRenderer = metaRendererClass.getField("INSTANCE").get(null);
                blazeColorWrapperField = metaRendererClass.getField("dhColorTextureWrapper");
                blazeDepthWrapperField = metaRendererClass.getField("dhDepthTextureWrapper");
            }

            Object colorWrapper = blazeColorWrapperField.get(blazeMetaRenderer);
            Object depthWrapper = blazeDepthWrapperField.get(blazeMetaRenderer);
            if (colorWrapper == null || depthWrapper == null) {
                return null;
            }
            if (blazeTextureViewMethod == null) {
                blazeTextureViewMethod = colorWrapper.getClass().getMethod("getTextureView");
            }

            GpuTextureView colorView =
                    (GpuTextureView)blazeTextureViewMethod.invoke(colorWrapper);
            GpuTextureView depthView =
                    (GpuTextureView)blazeTextureViewMethod.invoke(depthWrapper);
            if (colorView == null || depthView == null
                    || colorView.isClosed() || depthView.isClosed()) {
                return null;
            }
            return new ExternalFramebufferTarget(colorView, depthView);
        } catch (ReflectiveOperationException | ClassCastException | LinkageError exception) {
            blazeRendererLookupFailed = true;
            Cirrus.LOGGER.warn(
                    "Unable to access Distant Horizons Blaze textures; using Minecraft's normal cloud pass",
                    exception
            );
            return null;
        }
    }

    private record ExternalFramebufferTarget(
            GpuTextureView colorView,
            GpuTextureView depthView
    ) {
    }
}
