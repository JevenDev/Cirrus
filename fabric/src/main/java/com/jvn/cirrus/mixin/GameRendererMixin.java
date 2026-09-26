package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.config.CirrusConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.world.level.Level;
import com.jvn.cirrus.client.CirrusSky;
import com.jvn.cirrus.client.CirrusTimeTransition;
import com.jvn.cirrus.client.compat.distanthorizons.DistantHorizonsCompat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @WrapOperation(method = "renderLevel", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/BossHealthOverlay;shouldCreateWorldFog()Z"
    ))
    private boolean cirrus$replaceEndBossFog(BossHealthOverlay overlay, Operation<Boolean> original) {
        boolean bossFog = original.call(overlay);
        ClientLevel level = Minecraft.getInstance().level;
        return bossFog && !(level != null && Level.END.equals(level.dimension())
                && CirrusConfig.END_SKY_ENABLED.get());
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void cirrus$beginTimeTransitionFrame(DeltaTracker deltaTracker, CallbackInfo ci) {
        DistantHorizonsCompat.beginFrame();
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            CirrusTimeTransition.beginFrame(level);
        }
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void cirrus$endTimeTransitionFrame(DeltaTracker deltaTracker, CallbackInfo ci) {
        CirrusTimeTransition.endFrame();
    }

    @Inject(method = "getDepthFar", at = @At("RETURN"), cancellable = true)
    private void cirrus$keepCloudsInsideProjection(CallbackInfoReturnable<Float> cir) {
        boolean cirrusCloudsActive = CirrusCloudMode.isActive(
                Minecraft.getInstance().options.getCloudsType()
        );
        cir.setReturnValue(Math.max(
                cir.getReturnValueF(),
                CirrusSky.minimumFarPlane(cirrusCloudsActive)
        ));
    }
}
