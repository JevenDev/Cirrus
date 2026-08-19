package com.jvn.cirrus.client.util;

import com.jvn.cirrus.client.render.CirrusShader;

public final class CirrusShaderUniforms {
    private CirrusShaderUniforms() {
    }

    public static void setUniform(CirrusShader shader, String name, float value) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    public static void setUniform(CirrusShader shader, String name, float x, float y) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }

    public static void setUniform(CirrusShader shader, String name, float x, float y, float z) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }

    public static void setUniform(CirrusShader shader, String name, float x, float y, float z, float w) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z, w);
        }
    }

    public static void setUniform(CirrusShader shader, String name, boolean value) {
        setUniform(shader, name, value ? 1.0F : 0.0F);
    }
}
