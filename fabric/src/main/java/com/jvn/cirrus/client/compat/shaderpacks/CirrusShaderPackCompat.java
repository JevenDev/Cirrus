package com.jvn.cirrus.client.compat.shaderpacks;

import com.jvn.cirrus.Cirrus;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CirrusShaderPackCompat {
    private static final String[] API_CLASS_NAMES = {
            "net.irisshaders.iris.api.v0.IrisApi",
            "net.coderbot.iris.api.v0.IrisApi"
    };
    private static final String[] IRIS_CLASS_NAMES = {
            "net.irisshaders.iris.Iris",
            "net.coderbot.iris.Iris"
    };
    private static final boolean SHADER_LOADER_PRESENT =
            FabricLoader.getInstance().isModLoaded("iris")
                    || FabricLoader.getInstance().isModLoaded("oculus");
    private static final ApiAccess API = findApi();
    private static final IrisAccess IRIS = findIris();
    private static final AtomicBoolean INVOCATION_WARNING_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean SETTINGS_WARNING_LOGGED = new AtomicBoolean();

    private CirrusShaderPackCompat() {
    }

    public static boolean isShaderPackInUse() {
        if (API == null) {
            return false;
        }
        try {
            return (boolean)API.isShaderPackInUse().invoke(API.instance());
        } catch (ReflectiveOperationException | LinkageError exception) {
            logInvocationWarning(exception);
            return false;
        }
    }

    public static boolean assignCloudPipeline(RenderPipeline pipeline) {
        if (API == null || API.assignPipeline() == null || API.cloudMeshProgram() == null) {
            return false;
        }
        try {
            API.assignPipeline().invoke(API.instance(), pipeline, API.cloudMeshProgram());
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            logInvocationWarning(exception);
            return false;
        }
    }

    public static boolean hasCompatibilityProfile(String packName) {
        return PackProfile.forPackName(packName) != null;
    }

    public static Optional<String> currentPackName() {
        if (!isShaderPackInUse() || IRIS == null) {
            return Optional.empty();
        }
        try {
            String packName = (String)IRIS.getCurrentPackName().invoke(null);
            return Optional.ofNullable(packName).filter(name -> !name.isBlank());
        } catch (ReflectiveOperationException | LinkageError exception) {
            logInvocationWarning(exception);
            return Optional.empty();
        }
    }

    public static Optional<ActiveFix> activeFix() {
        Optional<ActivePack> activePack = activePack();
        if (activePack.isEmpty()) {
            return Optional.empty();
        }
        ActivePack pack = activePack.get();
        try {
            return Optional.of(new ActiveFix(
                    pack.packName(),
                    pack.profile().displayName(),
                    needsSettingsFix(pack.optionsPath(), pack.profile().requiredSettings())
            ));
        } catch (IOException exception) {
            if (SETTINGS_WARNING_LOGGED.compareAndSet(false, true)) {
                Cirrus.LOGGER.warn("Could not inspect shader pack compatibility settings", exception);
            }
            return Optional.empty();
        }
    }

    public static ApplyResult applyActiveFix() {
        Optional<ActivePack> activePack = activePack();
        if (activePack.isEmpty()) {
            return new ApplyResult(ApplyStatus.UNSUPPORTED, "");
        }
        ActivePack pack = activePack.get();
        try {
            if (!needsSettingsFix(pack.optionsPath(), pack.profile().requiredSettings())) {
                return new ApplyResult(ApplyStatus.ALREADY_COMPATIBLE, pack.profile().displayName());
            }
            patchSettingsFile(pack.optionsPath(), pack.profile().requiredSettings());
        } catch (IOException exception) {
            Cirrus.LOGGER.error("Could not update shader pack compatibility settings", exception);
            return new ApplyResult(ApplyStatus.FAILED, pack.profile().displayName());
        }

        try {
            IRIS.reload().invoke(null);
            return new ApplyResult(ApplyStatus.APPLIED, pack.profile().displayName());
        } catch (ReflectiveOperationException | LinkageError exception) {
            Cirrus.LOGGER.error("Saved shader pack compatibility settings but could not reload Iris", exception);
            return new ApplyResult(ApplyStatus.SAVED_RELOAD_FAILED, pack.profile().displayName());
        }
    }

    private static Optional<ActivePack> activePack() {
        if (IRIS == null) {
            return Optional.empty();
        }
        Optional<String> currentPackName = currentPackName();
        if (currentPackName.isEmpty()) {
            return Optional.empty();
        }
        String packName = currentPackName.get();
        PackProfile profile = PackProfile.forPackName(packName);
        if (profile == null) {
            return Optional.empty();
        }

        try {
            Path fileName = Path.of(packName).getFileName();
            if (fileName == null || !fileName.toString().equals(packName)) {
                return Optional.empty();
            }
            Path shaderpacksDirectory = ((Path)IRIS.getShaderpacksDirectory().invoke(null))
                    .toAbsolutePath()
                    .normalize();
            Path optionsPath = shaderpacksDirectory.resolve(packName + ".txt").normalize();
            if (!shaderpacksDirectory.equals(optionsPath.getParent())) {
                return Optional.empty();
            }
            return Optional.of(new ActivePack(packName, optionsPath, profile));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            logInvocationWarning(exception);
            return Optional.empty();
        }
    }

    private static boolean needsSettingsFix(Path optionsPath, Map<String, String> requiredSettings)
            throws IOException {
        if (!Files.isRegularFile(optionsPath)) {
            return true;
        }
        String content = Files.readString(optionsPath, StandardCharsets.ISO_8859_1);
        Properties properties = new Properties();
        properties.load(new StringReader(content));
        for (Map.Entry<String, String> setting : requiredSettings.entrySet()) {
            if (!setting.getValue().equals(properties.getProperty(setting.getKey()))) {
                return true;
            }
        }
        return false;
    }

    private static void patchSettingsFile(Path optionsPath, Map<String, String> requiredSettings)
            throws IOException {
        String content = Files.isRegularFile(optionsPath)
                ? Files.readString(optionsPath, StandardCharsets.ISO_8859_1)
                : "";
        String updated = content;
        for (Map.Entry<String, String> setting : requiredSettings.entrySet()) {
            updated = replaceSetting(updated, setting.getKey(), setting.getValue());
        }
        if (updated.equals(content)) {
            return;
        }

        Path directory = optionsPath.getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, optionsPath.getFileName().toString(), ".cirrus.tmp");
        try {
            Files.writeString(temporary, updated, StandardCharsets.ISO_8859_1);
            try {
                Files.move(
                        temporary,
                        optionsPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, optionsPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String replaceSetting(String content, String key, String value) {
        Pattern pattern = Pattern.compile(
                "(?m)^(\\s*" + Pattern.quote(key) + "\\s*[=:]\\s*)[^\\r\\n]*$"
        );
        Matcher matcher = pattern.matcher(content);
        StringBuffer replaced = new StringBuffer();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            matcher.appendReplacement(
                    replaced,
                    Matcher.quoteReplacement(matcher.group(1) + value)
            );
        }
        matcher.appendTail(replaced);
        if (found) {
            return replaced.toString();
        }

        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        String separator = content.isEmpty() || content.endsWith("\n") || content.endsWith("\r")
                ? ""
                : newline;
        return content + separator + key + "=" + value + newline;
    }

    private static void logInvocationWarning(Throwable exception) {
        if (INVOCATION_WARNING_LOGGED.compareAndSet(false, true)) {
            Cirrus.LOGGER.warn(
                    "Could not query the installed shader loader; using Cirrus's native compatibility behavior",
                    exception
            );
        }
    }

    private static ApiAccess findApi() {
        if (!SHADER_LOADER_PRESENT) {
            return null;
        }
        for (String className : API_CLASS_NAMES) {
            try {
                Class<?> apiClass = Class.forName(className);
                Method getInstance = apiClass.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                Method isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
                Method assignPipeline = null;
                Object cloudMeshProgram = null;
                try {
                    Class<?> programClass = Class.forName(
                            className.substring(0, className.lastIndexOf('.') + 1) + "IrisProgram"
                    );
                    assignPipeline = apiClass.getMethod(
                            "assignPipeline", RenderPipeline.class, programClass
                    );
                    @SuppressWarnings({"rawtypes", "unchecked"})
                    Object program = Enum.valueOf((Class<? extends Enum>)programClass, "TEXTURED");
                    cloudMeshProgram = program;
                } catch (ClassNotFoundException | NoSuchMethodException | IllegalArgumentException ignored) {
                }
                return new ApiAccess(instance, isShaderPackInUse, assignPipeline, cloudMeshProgram);
            } catch (ClassNotFoundException ignored) {
                // Try the next known Iris/Oculus API package.
            } catch (ReflectiveOperationException | LinkageError exception) {
                Cirrus.LOGGER.warn(
                        "Found a shader loader but could not initialize its compatibility API",
                        exception
                );
                return null;
            }
        }
        Cirrus.LOGGER.warn("Found a shader loader but no supported Iris/Oculus compatibility API");
        return null;
    }

    private static IrisAccess findIris() {
        if (!SHADER_LOADER_PRESENT) {
            return null;
        }
        for (String className : IRIS_CLASS_NAMES) {
            try {
                Class<?> irisClass = Class.forName(className);
                return new IrisAccess(
                        irisClass.getMethod("getCurrentPackName"),
                        irisClass.getMethod("getShaderpacksDirectory"),
                        irisClass.getMethod("reload")
                );
            } catch (ClassNotFoundException ignored) {
                // Try the next known Iris/Oculus implementation package.
            } catch (ReflectiveOperationException | LinkageError exception) {
                Cirrus.LOGGER.warn(
                        "Found a shader loader but could not initialize pack-settings compatibility",
                        exception
                );
                return null;
            }
        }
        return null;
    }

    public record ActiveFix(String packName, String displayName, boolean needsChanges) {
    }

    public record ApplyResult(ApplyStatus status, String displayName) {
    }

    public enum ApplyStatus {
        APPLIED,
        ALREADY_COMPATIBLE,
        SAVED_RELOAD_FAILED,
        UNSUPPORTED,
        FAILED
    }

    private enum PackProfile {
        COMPLEMENTARY_REIMAGINED(
                "Complementary Reimagined",
                Map.of(
                        "CLOUD_STYLE_DEFINE", "50",
                        "BORDER_FOG", "false"
                )
        ),
        COMPLEMENTARY_UNBOUND(
                "Complementary Unbound",
                Map.of(
                        "CLOUD_STYLE_DEFINE", "50",
                        "BORDER_FOG", "false"
                )
        ),
        RETHINKING_VOXELS(
                "Rethinking Voxels",
                Map.of(
                        "CLOUD_STYLE_DEFINE", "50",
                        "BORDER_FOG", "false"
                )
        ),
        BSL(
                "BSL",
                Map.of(
                        "CLOUDS", "3",
                        "FOG_VANILLA_CLOUD", "0"
                )
        ),
        INSANITY(
                "Insanity",
                Map.of("CLOUDS", "2")
        ),
        MAKEUP_ULTRA_FAST(
                "MakeUp Ultra Fast",
                Map.of("V_CLOUDS", "0")
        ),
        MELLOW(
                "Mellow",
                Map.of(
                        "CLOUD_STYLE", "0",
                        "BORDER_FOG", "false"
                )
        );

        private final String displayName;
        private final Map<String, String> requiredSettings;

        PackProfile(String displayName, Map<String, String> requiredSettings) {
            this.displayName = displayName;
            this.requiredSettings = requiredSettings;
        }

        private String displayName() {
            return displayName;
        }

        private Map<String, String> requiredSettings() {
            return requiredSettings;
        }

        private static PackProfile forPackName(String packName) {
            String normalized = packName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (normalized.contains("complementaryreimagined")) {
                return COMPLEMENTARY_REIMAGINED;
            }
            if (normalized.contains("complementaryunbound")) {
                return COMPLEMENTARY_UNBOUND;
            }
            if (normalized.contains("rethinkingvoxels")) {
                return RETHINKING_VOXELS;
            }
            if (normalized.startsWith("bsl")) {
                return BSL;
            }
            if (normalized.startsWith("insanityshader")) {
                return INSANITY;
            }
            if (normalized.startsWith("makeupultrafast")) {
                return MAKEUP_ULTRA_FAST;
            }
            if (normalized.startsWith("mellowshader")) {
                return MELLOW;
            }
            return null;
        }
    }

    private record ActivePack(String packName, Path optionsPath, PackProfile profile) {
    }

    private record ApiAccess(
            Object instance,
            Method isShaderPackInUse,
            Method assignPipeline,
            Object cloudMeshProgram
    ) {
    }

    private record IrisAccess(Method getCurrentPackName, Method getShaderpacksDirectory, Method reload) {
    }
}
