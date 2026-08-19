package com.jvn.cirrus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CirrusConfigSpec {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, ConfigValue<?>> values;
    private Path configPath;

    private CirrusConfigSpec(Map<String, ConfigValue<?>> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public synchronized void load(Path path) {
        configPath = path.toAbsolutePath().normalize();
        reset();

        if (Files.isRegularFile(configPath)) {
            loadJson();
            return;
        }

        Path legacyPath = configPath.resolveSibling("cirrus-client.toml");
        if (Files.isRegularFile(legacyPath)) {
            loadLegacyToml(legacyPath);
            LOGGER.info("Migrated Cirrus configuration from {} to {}", legacyPath, configPath);
        }
        save();
    }

    public synchronized void save() {
        if (configPath == null) {
            return;
        }

        JsonObject root = new JsonObject();
        values.forEach((key, value) -> put(root, key, value.toJson()));

        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporary = configPath.resolveSibling(configPath.getFileName() + ".tmp");
            Files.writeString(
                    temporary,
                    GSON.toJson(root) + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );
            try {
                Files.move(
                        temporary,
                        configPath,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, configPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Unable to save Cirrus configuration to {}", configPath, exception);
        }
    }

    private void loadJson() {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IOException("The root value must be a JSON object");
            }
            JsonObject root = parsed.getAsJsonObject();
            values.forEach((key, value) -> value.load(find(root, key)));
        } catch (Exception exception) {
            LOGGER.error("Unable to load Cirrus configuration from {}; using defaults", configPath, exception);
        }
    }

    private void loadLegacyToml(Path legacyPath) {
        String section = "";
        try {
            for (String sourceLine : Files.readAllLines(legacyPath, StandardCharsets.UTF_8)) {
                String line = sourceLine.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1).strip();
                    continue;
                }

                int separator = line.indexOf('=');
                if (separator < 1) {
                    continue;
                }
                String name = line.substring(0, separator).strip();
                String rawValue = stripComment(line.substring(separator + 1)).strip();
                String key = section.isEmpty() ? name : section + "." + name;
                ConfigValue<?> value = values.get(key);
                if (value != null) {
                    value.loadLegacy(rawValue);
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Unable to migrate legacy Cirrus configuration from {}", legacyPath, exception);
        }
    }

    private void reset() {
        values.values().forEach(ConfigValue::reset);
    }

    private static String stripComment(String value) {
        boolean quoted = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (character == '#' && !quoted) {
                return value.substring(0, index);
            }
        }
        return value;
    }

    private static JsonElement find(JsonObject root, String key) {
        String[] segments = key.split("\\.");
        JsonElement current = root;
        for (String segment : segments) {
            if (!current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static void put(JsonObject root, String key, JsonElement value) {
        String[] segments = key.split("\\.");
        JsonObject current = root;
        for (int index = 0; index < segments.length - 1; index++) {
            String segment = segments[index];
            JsonElement child = current.get(segment);
            if (child == null || !child.isJsonObject()) {
                JsonObject object = new JsonObject();
                current.add(segment, object);
                current = object;
            } else {
                current = child.getAsJsonObject();
            }
        }
        current.add(segments[segments.length - 1], value);
    }

    public static final class Builder {
        private final Deque<String> sections = new ArrayDeque<>();
        private final Map<String, ConfigValue<?>> values = new LinkedHashMap<>();

        public Builder comment(String ignored) {
            return this;
        }

        public Builder push(String section) {
            sections.addLast(section);
            return this;
        }

        public Builder pop() {
            if (sections.isEmpty()) {
                throw new IllegalStateException("Cannot pop an empty config section");
            }
            sections.removeLast();
            return this;
        }

        public BooleanValue define(String name, boolean defaultValue) {
            return register(name, new BooleanValue(key(name), defaultValue));
        }

        public IntValue defineInRange(String name, int defaultValue, int minimum, int maximum) {
            return register(name, new IntValue(key(name), defaultValue, minimum, maximum));
        }

        public DoubleValue defineInRange(String name, double defaultValue, double minimum, double maximum) {
            return register(name, new DoubleValue(key(name), defaultValue, minimum, maximum));
        }

        public <E extends Enum<E>> EnumValue<E> defineEnum(String name, E defaultValue) {
            return register(name, new EnumValue<>(key(name), defaultValue));
        }

        public CirrusConfigSpec build() {
            return new CirrusConfigSpec(values);
        }

        private String key(String name) {
            return sections.isEmpty() ? name : String.join(".", sections) + "." + name;
        }

        private <V extends ConfigValue<?>> V register(String name, V value) {
            if (values.putIfAbsent(value.key(), value) != null) {
                throw new IllegalArgumentException("Duplicate config value " + key(name));
            }
            return value;
        }
    }

    private abstract static class ConfigValue<T> {
        private final String key;
        private final T defaultValue;
        private T value;

        private ConfigValue(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            value = defaultValue;
        }

        final String key() {
            return key;
        }

        public final T get() {
            return value;
        }

        public final T getDefault() {
            return defaultValue;
        }

        public final void set(T value) {
            this.value = sanitize(value);
        }

        private void reset() {
            value = defaultValue;
        }

        private void load(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return;
            }
            try {
                set(read(element));
            } catch (RuntimeException exception) {
                LOGGER.warn("Ignoring invalid Cirrus config value for {}", key);
            }
        }

        private void loadLegacy(String rawValue) {
            String value = rawValue;
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            load(new com.google.gson.JsonPrimitive(value));
        }

        protected T sanitize(T value) {
            return value == null ? defaultValue : value;
        }

        protected abstract T read(JsonElement element);

        protected abstract JsonElement toJson();
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        private BooleanValue(String key, boolean defaultValue) {
            super(key, defaultValue);
        }

        @Override
        protected Boolean read(JsonElement element) {
            return element.getAsBoolean();
        }

        @Override
        protected JsonElement toJson() {
            return new com.google.gson.JsonPrimitive(get());
        }
    }

    public static final class IntValue extends ConfigValue<Integer> {
        private final int minimum;
        private final int maximum;

        private IntValue(String key, int defaultValue, int minimum, int maximum) {
            super(key, defaultValue);
            if (maximum < minimum) {
                throw new IllegalArgumentException("maximum must not be below minimum");
            }
            this.minimum = minimum;
            this.maximum = maximum;
            set(defaultValue);
        }

        @Override
        protected Integer sanitize(Integer value) {
            int number = value == null ? getDefault() : value;
            return Math.max(minimum, Math.min(maximum, number));
        }

        @Override
        protected Integer read(JsonElement element) {
            return element.getAsInt();
        }

        @Override
        protected JsonElement toJson() {
            return new com.google.gson.JsonPrimitive(get());
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> {
        private final double minimum;
        private final double maximum;

        private DoubleValue(String key, double defaultValue, double minimum, double maximum) {
            super(key, defaultValue);
            if (maximum < minimum) {
                throw new IllegalArgumentException("maximum must not be below minimum");
            }
            this.minimum = minimum;
            this.maximum = maximum;
            set(defaultValue);
        }

        @Override
        protected Double sanitize(Double value) {
            double number = value == null || !Double.isFinite(value) ? getDefault() : value;
            return Math.max(minimum, Math.min(maximum, number));
        }

        @Override
        protected Double read(JsonElement element) {
            return element.getAsDouble();
        }

        @Override
        protected JsonElement toJson() {
            return new com.google.gson.JsonPrimitive(get());
        }
    }

    public static final class EnumValue<E extends Enum<E>> extends ConfigValue<E> {
        private final Class<E> enumClass;

        @SuppressWarnings("unchecked")
        private EnumValue(String key, E defaultValue) {
            super(key, defaultValue);
            enumClass = (Class<E>) defaultValue.getDeclaringClass();
        }

        @Override
        protected E read(JsonElement element) {
            return Enum.valueOf(enumClass, element.getAsString().toUpperCase(Locale.ROOT));
        }

        @Override
        protected JsonElement toJson() {
            return new com.google.gson.JsonPrimitive(get().name().toLowerCase(Locale.ROOT));
        }
    }
}
