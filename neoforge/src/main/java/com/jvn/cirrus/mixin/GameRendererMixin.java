package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudMode;
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
