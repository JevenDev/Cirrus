package com.jvn.cirrus.mixin;

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

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "extract", at = @At("HEAD"))
    private void cirrus$beginTimeTransitionExtraction(
            DeltaTracker deltaTracker,
            boolean advanceGameTime,
            CallbackInfo ci
    ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (advanceGameTime && level != null) {
            CirrusTimeTransition.beginFrame(level);
        }
    }

    @Inject(method = "extract", at = @At("RETURN"))
    private void cirrus$endTimeTransitionExtraction(
            DeltaTracker deltaTracker,
            boolean advanceGameTime,
            CallbackInfo ci
    ) {
        CirrusTimeTransition.endFrame();
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
}
