package com.jvn.cirrus.client.compat.distanthorizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig;
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue;
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeApplyShaderRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;

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
    private static final DhApiBeforeApplyShaderRenderEvent BEFORE_APPLY_SHADER_EVENT =
            new DhApiBeforeApplyShaderRenderEvent() {
        @Override
        public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
            Consumer<float[]> callback = beforeApplyShaderCallback;
            if (callback != null) {
                event.value.dhProjectionMatrix.putValuesInArray(DH_PROJECTION_MATRIX_VALUES);
                callback.accept(DH_PROJECTION_MATRIX_VALUES);
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
}
