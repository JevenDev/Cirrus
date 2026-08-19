package com.jvn.cirrus.client;

import net.minecraft.world.phys.Vec3;

public interface CirrusLightningRenderStateAccess {
    void cirrus$setLightningData(float age, Vec3 visualOrigin);

    float cirrus$age();

    Vec3 cirrus$visualOrigin();
}
