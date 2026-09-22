package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.CirrusTimeTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "net.mehvahdjukaar.polytone.content.shaders.PolytoneGlobalUniforms", remap = false)
public abstract class PolytoneGlobalUniformsMixin {
    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, ordinal = 1, remap = false)
    private float cirrus$useVisualDayTime(float dayTime) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (!CirrusTimeTransition.canUseVisualTime(level)) {
            return dayTime;
        }
        return (float)(CirrusTimeTransition.visualDayTime() % 24000L);
    }
}
