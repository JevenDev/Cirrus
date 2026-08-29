package com.jvn.cirrus.mixin;

import com.jvn.cirrus.client.util.CirrusCelestialTransform;
import com.jvn.cirrus.config.CirrusConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererCelestialMixin {
    @Shadow private ClientLevel level;
    @Unique private final Matrix4f cirrus$sunModelView = new Matrix4f();
    @Unique private final Matrix4f cirrus$moonModelView = new Matrix4f();
    @Unique private final Matrix4f cirrus$sunriseModelView = new Matrix4f();
    @Unique private boolean cirrus$sunAngledOrbit;
    @Unique private boolean cirrus$moonAngledOrbit;

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void cirrus$prepareCelestialOrbits(
            Matrix4f frustumMatrix,
            Matrix4f projectionMatrix,
            float partialTick,
            Camera camera,
            boolean isFoggy,
            Runnable skyFogSetup,
            CallbackInfo ci
    ) {
        if (level == null) {
            return;
        }
        float timeOfDay = level.getTimeOfDay(partialTick);
        cirrus$sunAngledOrbit = CirrusConfig.SUN_ANGLED_ORBIT.get();
        cirrus$moonAngledOrbit = CirrusConfig.MOON_ANGLED_ORBIT.get();
        CirrusCelestialTransform.sunriseModelView(
                cirrus$sunriseModelView,
                frustumMatrix,
                timeOfDay
        );
        CirrusCelestialTransform.bodyModelView(
                cirrus$sunModelView,
                frustumMatrix,
                timeOfDay,
                cirrus$sunAngledOrbit
        );
        CirrusCelestialTransform.bodyModelView(
                cirrus$moonModelView,
                frustumMatrix,
                timeOfDay,
                cirrus$moonAngledOrbit
        );
    }

    @ModifyArg(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(" +
                            "Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;" +
                                    "getSunriseColor(FF)[F"
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(" +
                                    "Lcom/mojang/blaze3d/vertex/MeshData;)V",
                            ordinal = 0
                    )
            ),
            index = 0
    )
    private Matrix4f cirrus$rotateSunrise(Matrix4f original) {
        return cirrus$sunAngledOrbit ? cirrus$sunriseModelView : original;
    }

    @ModifyArg(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(" +
                            "Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(" +
                                    "ILnet/minecraft/resources/ResourceLocation;)V",
                            ordinal = 0
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(" +
                                    "Lcom/mojang/blaze3d/vertex/MeshData;)V",
                            ordinal = 1
                    )
            ),
            index = 0
    )
    private Matrix4f cirrus$rotateSun(Matrix4f original) {
        return cirrus$sunAngledOrbit ? cirrus$sunModelView : original;
    }

    @ModifyArg(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;addVertex(" +
                            "Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(" +
                                    "ILnet/minecraft/resources/ResourceLocation;)V",
                            ordinal = 1
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(" +
                                    "Lcom/mojang/blaze3d/vertex/MeshData;)V",
                            ordinal = 2
                    )
            ),
            index = 0
    )
    private Matrix4f cirrus$rotateMoon(Matrix4f original) {
        return cirrus$moonAngledOrbit ? cirrus$moonModelView : original;
    }
}
