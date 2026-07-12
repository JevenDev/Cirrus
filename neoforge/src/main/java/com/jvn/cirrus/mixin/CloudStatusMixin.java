package com.jvn.cirrus.mixin;

import java.util.Arrays;
import net.minecraft.client.CloudStatus;
import net.minecraft.util.StringRepresentable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CloudStatus.class)
public abstract class CloudStatusMixin {
    @Shadow @Final @Mutable private static CloudStatus[] $VALUES;

    @Invoker("<init>")
    private static CloudStatus cirrus$create(
            String name,
            int ordinal,
            int id,
            String serializedName,
            String translationKey
    ) {
        throw new AssertionError();
    }

    @Inject(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/StringRepresentable;fromEnum(Ljava/util/function/Supplier;)Lnet/minecraft/util/StringRepresentable$EnumCodec;"
            )
    )
    private static void cirrus$addCloudMode(CallbackInfo ci) {
        CloudStatus cirrus = cirrus$create(
                "CIRRUS",
                $VALUES.length,
                $VALUES.length,
                "cirrus",
                "options.clouds.cirrus"
        );
        $VALUES = Arrays.copyOf($VALUES, $VALUES.length + 1);
        $VALUES[$VALUES.length - 1] = cirrus;
    }
}
