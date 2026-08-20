package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusCloudMode;
import com.jvn.cirrus.client.CirrusSky;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private float depthFar;

    @Inject(
            method = "update",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Camera;depthFar:F",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void cirrus$extendFarPlane(DeltaTracker deltaTracker, CallbackInfo ci) {
        boolean active = CirrusCloudMode.isActive(Minecraft.getInstance().options.getCloudStatus());
        depthFar = Math.max(depthFar, CirrusSky.minimumFarPlane(active));
    }
}
