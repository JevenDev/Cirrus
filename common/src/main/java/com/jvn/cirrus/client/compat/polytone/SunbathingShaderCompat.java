package com.jvn.cirrus.client.compat.polytone;

import com.jvn.cirrus.Cirrus;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SunbathingShaderCompat {
    private static final Pattern SUN_ANGLE = Pattern.compile("uniform\\s+float\\s+PolySunAngle\\s*;");
    private static final Pattern DIRECTION = Pattern.compile(
            "vec3\\s+dir\\s*=\\s*vec3\\s*\\(\\s*cos\\s*\\(\\s*(angle|PolySunAngle)\\s*\\)"
                    + "\\s*,\\s*sin\\s*\\(\\s*\\1\\s*\\)\\s*,\\s*0\\.0\\s*\\)\\s*;"
    );

    private SunbathingShaderCompat() {
    }

    public static boolean isSupported(String name) {
        return "sunbathing:godrays".equals(name) || "sunbathing:lens_flare".equals(name);
    }

    public static String adapt(String name, String source) {
        if (!isSupported(name) || source.contains("uniform vec3 CirrusSunDirection;")) {
            return source;
        }
        Matcher direction = DIRECTION.matcher(source);
        Matcher angle = SUN_ANGLE.matcher(source);
        if (!direction.find() || !angle.find()) {
            Cirrus.LOGGER.warn("Could not adapt celestial directions in Sunbathing shader {}", name);
            return source;
        }
        // legacy Sunbathing assumes opposing bodies on the vanilla orbit
        String replacement = "vec3 dir = cos(" + direction.group(1) + " - PolySunAngle) >= 0.0"
                + " ? -CirrusSunDirection : -CirrusMoonDirection;";
        String adapted = direction.replaceFirst(Matcher.quoteReplacement(replacement));
        return SUN_ANGLE.matcher(adapted).replaceFirst(Matcher.quoteReplacement(
                "uniform float PolySunAngle;\nuniform vec3 CirrusSunDirection;\nuniform vec3 CirrusMoonDirection;"
        ));
    }
}
