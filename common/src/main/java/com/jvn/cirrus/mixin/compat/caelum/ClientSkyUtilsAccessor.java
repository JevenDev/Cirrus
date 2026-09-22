package com.jvn.cirrus.mixin.compat.caelum;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.sophka.caelum.client.ClientSkyUtils", remap = false)
public interface ClientSkyUtilsAccessor {
    @Invoker("moonVector")
    static Vector3f cirrus$moonDirection() {
        throw new AssertionError();
    }
}
