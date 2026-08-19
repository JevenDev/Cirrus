package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.CirrusTimeTransition;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void cirrus$beginLoginLevelLoad(ClientboundLoginPacket packet, CallbackInfo ci) {
        CirrusTimeTransition.beginLevelLoad();
    }

    @Inject(method = "handleRespawn", at = @At("TAIL"))
    private void cirrus$beginRespawnLevelLoad(ClientboundRespawnPacket packet, CallbackInfo ci) {
        CirrusTimeTransition.beginLevelLoad();
    }
}
