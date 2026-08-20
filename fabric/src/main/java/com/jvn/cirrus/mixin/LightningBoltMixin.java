package com.jvn.cirrus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {
    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void cirrus$useConfiguredRenderDistance(
            double vanillaDistanceSquared,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 cameraPosition = minecraft.gameRenderer.mainCamera().position();
        LightningBolt lightning = (LightningBolt)(Object)this;
        double offsetX = lightning.getX() - cameraPosition.x;
        double offsetZ = lightning.getZ() - cameraPosition.z;
        double renderDistance = minecraft.options.getEffectiveRenderDistance() * 16.0;
        cir.setReturnValue(offsetX * offsetX + offsetZ * offsetZ <= renderDistance * renderDistance);
    }
}
