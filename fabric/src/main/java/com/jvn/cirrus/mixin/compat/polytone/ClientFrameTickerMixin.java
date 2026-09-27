package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.CirrusTimeTransition;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.polytone.misc.ClientFrameTicker", remap = false)
public abstract class ClientFrameTickerMixin {
    @ModifyReturnValue(method = "getDayTime", at = @At("RETURN"), remap = false, require = 0)
    private static double cirrus$useVisualDayTime(double original) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        return CirrusTimeTransition.canUseVisualTime(level)
                ? CirrusTimeTransition.visualDayTime(
                        level, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false))
                : original;
    }
}
