package com.jvn.cirrus.client.compat.polytone;

import com.jvn.cirrus.Cirrus;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SunbathingShaderCompat {
    private static final Pattern DIRECTION = Pattern.compile(
            "vec3\\s+dir\\s*=\\s*vec3\\s*\\(\\s*cos\\s*\\(\\s*(angle|PolySunAngle)\\s*\\)"
                    + "\\s*,\\s*sin\\s*\\(\\s*\\1\\s*\\)\\s*,\\s*0\\.0\\s*\\)\\s*;"
    );

    private SunbathingShaderCompat() {
    }

    public static boolean isSupported(String name) {
        return "sunbathing:post/godrays".equals(name) || "sunbathing:post/lens_flare".equals(name);
    }

    public static String adapt(String name, String source) {
        if (!isSupported(name) || source.contains("uniform CirrusCelestial")) {
            return source;
        }
        Matcher direction = DIRECTION.matcher(source);
        if (!direction.find()) {
            Cirrus.LOGGER.warn("Could not adapt celestial directions in Sunbathing shader {}", name);
            return source;
        }
        String replacement = "vec3 dir = cos(" + direction.group(1) + " - PolySunAngle) >= 0.0"
                + " ? -CirrusSunDirection.xyz : -CirrusMoonDirection.xyz;";
        String adapted = direction.replaceFirst(Matcher.quoteReplacement(replacement));
        int versionEnd = adapted.indexOf('\n') + 1;
        return adapted.substring(0, versionEnd)
                + "layout(std140) uniform CirrusCelestial { vec4 CirrusSunDirection; vec4 CirrusMoonDirection; };\n"
                + adapted.substring(versionEnd);
    }
}
