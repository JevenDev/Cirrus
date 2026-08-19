package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusLightningRenderStateAccess;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LightningBoltRenderState.class)
public abstract class LightningBoltRenderStateMixin implements CirrusLightningRenderStateAccess {
    @Unique private float cirrus$age;
    @Unique private Vec3 cirrus$visualOrigin = Vec3.ZERO;

    @Override
    public void cirrus$setLightningData(float age, Vec3 visualOrigin) {
        cirrus$age = age;
        cirrus$visualOrigin = visualOrigin;
    }

    @Override
    public float cirrus$age() {
        return cirrus$age;
    }

    @Override
    public Vec3 cirrus$visualOrigin() {
        return cirrus$visualOrigin;
    }
}
