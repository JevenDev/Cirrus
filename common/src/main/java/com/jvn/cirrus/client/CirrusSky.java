package com.jvn.cirrus.client;

import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector3f;

public final class CirrusSky {
    private static final float CLOUD_CLIP_MARGIN = 96.0F;

    private CirrusSky() {
    }

    public static boolean enabled(ClientLevel level) {
        return level != null && CirrusCloudDimensions.skyEnabled(level.dimension().location());
    }

    public static boolean matchesFog(Camera camera, ClientLevel level) {
        return enabled(level)
                && CirrusConfig.FOG_USES_HORIZON_COLOR.get()
                && CirrusConfig.SKY_GRADIENTS_ENABLED.get()
                && camera.getFluidInCamera() == FogType.NONE
                && level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL
                && !hasVisibilityEffect(camera);
    }

    public static boolean matchesFogColor(Camera camera, ClientLevel level) {
        return matchesFog(camera, level)
                && CirrusConfig.SKY_GRADIENT_OPACITY.get() > 0.0
                && CirrusConfig.FOG_HORIZON_TINT_STRENGTH.get() > 0.0;
    }

    public static boolean hasVisibilityEffect(Camera camera) {
        return camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS)
                        || living.hasEffect(MobEffects.DARKNESS));
    }

    public static Vector3f horizonDirection(Camera camera) {
        float yaw = camera.getYRot() * Mth.DEG_TO_RAD;
        return new Vector3f(-Mth.sin(yaw), 0.0F, Mth.cos(yaw));
    }

    public static float minimumFarPlane(boolean cirrusCloudsActive) {
        if (!cirrusCloudsActive) {
            return 0.0F;
        }

        float cloudDistance = DistantHorizonsCompat.cloudRenderDistanceChunks() * 16.0F;
        float lowerOffset = Math.abs(CirrusConfig.LOWER_LAYER_HEIGHT_OFFSET.get().floatValue());
        boolean topEnabled = CirrusConfig.TOP_LAYER_ENABLED.get();
        float upperOffset = CirrusConfig.UPPER_LAYER_ENABLED.get() || topEnabled
                ? CirrusConfig.UPPER_LAYER_HEIGHT_OFFSET.get().floatValue()
                : 0.0F;
        float topOffset = topEnabled
                ? CirrusConfig.TOP_LAYER_HEIGHT_OFFSET.get().floatValue()
                : 0.0F;
        float verticalAllowance = 256.0F + lowerOffset + upperOffset + topOffset;
        return (float)Math.hypot(cloudDistance, verticalAllowance) + CLOUD_CLIP_MARGIN;
    }
}
