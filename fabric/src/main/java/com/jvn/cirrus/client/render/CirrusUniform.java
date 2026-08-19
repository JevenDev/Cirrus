package com.jvn.cirrus.client.render;

import java.util.Arrays;

public final class CirrusUniform {
    private final String name;
    private final int components;
    private final float[] values;

    CirrusUniform(String name, int components, float[] defaults) {
        this.name = name;
        this.components = components;
        this.values = Arrays.copyOf(defaults, components);
    }

    public void set(float value) {
        setValues(value);
    }

    public void set(float x, float y) {
        setValues(x, y);
    }

    public void set(float x, float y, float z) {
        setValues(x, y, z);
    }

    public void set(float x, float y, float z, float w) {
        setValues(x, y, z, w);
    }

    private void setValues(float... newValues) {
        if (newValues.length != components) {
            throw new IllegalArgumentException(
                    "Uniform " + name + " expects " + components + " values, got " + newValues.length
            );
        }
        System.arraycopy(newValues, 0, values, 0, components);
    }

    int components() {
        return components;
    }

    float[] values() {
        return values;
    }
}
