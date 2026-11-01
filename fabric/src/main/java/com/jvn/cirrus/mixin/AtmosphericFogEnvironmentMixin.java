package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusSky;
import com.jvn.cirrus.config.CirrusConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.level.Level;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {
    @WrapOperation(method = "getBaseColor", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;forwardVector()Lorg/joml/Vector3fc;"
    ))
    private Vector3fc cirrus$useHorizonViewDirection(Camera camera, Operation<Vector3fc> original) {
        return CirrusSky.matchesFogColor(camera, Minecraft.getInstance().level)
                ? CirrusSky.horizonDirection(camera) : original.call(camera);
    }

    @WrapOperation(method = "setupFog", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;shouldCreateWorldFog()Z"
    ))
    private boolean cirrus$replaceEndBossFog(BossHealthOverlay overlay, Operation<Boolean> original) {
        boolean bossFog = original.call(overlay);
        ClientLevel level = Minecraft.getInstance().level;
        return bossFog && !(CirrusSky.enabled(level) && Level.END.equals(level.dimension())
                && (CirrusConfig.END_SKY_ENABLED.get() || CirrusConfig.END_GLASS_ENABLED.get()));
    }
}
