package com.jvn.cirrus.mixin.compat.polytone;

import com.jvn.cirrus.client.compat.polytone.SunbathingShaderCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import java.io.InputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Program.class)
public abstract class ProgramMixin {
    @ModifyExpressionValue(
            method = "compileShaderInternal",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/commons/io/IOUtils;toString("
                            + "Ljava/io/InputStream;Ljava/nio/charset/Charset;)Ljava/lang/String;",
                    remap = false
            )
    )
    private static String cirrus$useRenderedCelestialDirections(
            String source,
            Program.Type type,
            String name,
            InputStream stream,
            String pack,
            GlslPreprocessor preprocessor
    ) {
        return type == Program.Type.FRAGMENT ? SunbathingShaderCompat.adapt(name, source) : source;
    }
}
