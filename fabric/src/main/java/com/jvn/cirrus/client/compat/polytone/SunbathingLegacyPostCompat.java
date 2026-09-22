package com.jvn.cirrus.client.compat.polytone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jvn.cirrus.Cirrus;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import java.io.IOException;
import java.io.Reader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class SunbathingLegacyPostCompat {
    private static final ResourceLocation ACTIVATION = ResourceLocation.fromNamespaceAndPath(
            "sunbathing", "polytone/post_shaders/godrays.json"
    );
    private static final ResourceLocation CHAIN = ResourceLocation.fromNamespaceAndPath("sunbathing", "godrays");
    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    private static final boolean NATIVE_POST_CHAINS = SunbathingLegacyPostCompat.class.getClassLoader()
            .getResource("net/mehvahdjukaar/polytone/content/shaders/PostChainsManager.class") != null;
    private static boolean checked;
    private static boolean enabled;
    private static boolean frameReady;
    private static float sunAngle;

    private SunbathingLegacyPostCompat() {
    }

    public static boolean needsBridge() {
        return !NATIVE_POST_CHAINS && FabricLoader.getInstance().isModLoaded("polytone");
    }

    public static void reload() {
        checked = false;
        enabled = false;
        frameReady = false;
    }

    public static void capture(Matrix4f projection, Matrix4f view, float angle) {
        if (!needsBridge()) {
            return;
        }
        PROJECTION.set(projection);
        VIEW.set(view);
        sunAngle = angle - Mth.HALF_PI;
        frameReady = true;
    }

    public static void writeGlobals(Std140Builder builder) {
        builder.putMat4f(PROJECTION);
        builder.putMat4f(VIEW);
        builder.putFloat(sunAngle);
    }

    public static void render(CrossFrameResourcePool pool, float partialTick) {
        if (!needsBridge() || !frameReady) {
            return;
        }
        frameReady = false;
        Minecraft minecraft = Minecraft.getInstance();
        if (!checked) {
            checked = true;
            var activation = minecraft.getResourceManager().getResource(ACTIVATION);
            if (activation.isPresent()) {
                try (Reader reader = activation.get().openAsReader()) {
                    JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                    enabled = config.has("post_chain") && config.has("activation_condition")
                            && "sunbathing:godrays".equals(config.get("post_chain").getAsString())
                            && "g.skyType()==1&&g.rain()<=0.1".equals(
                                    config.get("activation_condition").getAsString().replaceAll("\\s+", "")
                            );
                    if (!enabled) {
                        Cirrus.LOGGER.warn("Sunbathing activation requires a newer Polytone post-chain implementation");
                    }
                } catch (IOException | com.google.gson.JsonParseException | IllegalStateException exception) {
                    Cirrus.LOGGER.warn("Could not read Sunbathing post-chain activation", exception);
                }
            }
        }
        if (!enabled || minecraft.level == null
                || minecraft.level.effects().skyType() != DimensionSpecialEffects.SkyType.OVERWORLD
                || minecraft.level.getRainLevel(partialTick) > 0.1F) {
            return;
        }
        PostChain chain = minecraft.getShaderManager().getPostChain(CHAIN, LevelTargetBundle.MAIN_TARGETS);
        if (chain != null) {
            chain.process(minecraft.getMainRenderTarget(), pool);
        }
    }
}
