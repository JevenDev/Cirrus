package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.CirrusTimeTransition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.polytone.content.shaders.PostShaderRenderer", remap = false)
public abstract class PostShaderRendererMixin {
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getSunAngle(F)F",
                    remap = true
            ),
            remap = false
    )
    private float cirrus$useVisualSunAngle(ClientLevel level, float partialTick, Operation<Float> original) {
        return CirrusTimeTransition.canUseVisualTime(level)
                ? level.dimensionType().timeOfDay(CirrusTimeTransition.visualDayTime()) * Mth.TWO_PI
                : original.call(level, partialTick);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getDayTime()J",
                    remap = true
            ),
            remap = false
    )
    private long cirrus$useVisualDayTime(ClientLevel level, Operation<Long> original) {
        return CirrusTimeTransition.canUseVisualTime(level)
                ? CirrusTimeTransition.visualDayTime()
                : original.call(level);
    }
}
