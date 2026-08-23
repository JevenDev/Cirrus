#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    float CirrusMilkyWayIntensity;
    float CirrusMilkyWayPixelation;
    float CirrusMilkyWayPixelationResolution;
    float CirrusSkyGradientIntensity;
    float CirrusSkyGradientHeight;
    vec3 CirrusSkyHorizonColor;
    vec3 CirrusSkyZenithColor;
    float CirrusMilkyWayRotation;
};

uniform sampler2D Sampler0;

in vec3 worldDirection;

out vec4 fragColor;

const float CIRRUS_PI = 3.14159265358979323846;
const float CIRRUS_TWO_PI = 6.28318530717958647692;
const float CIRRUS_LUMINOSITY_RANGE = 1.32;
const float CIRRUS_QUARTER_TURN = CIRRUS_PI * 0.5;

float cirrusMilkyWayPixelsPerFace() { return max(CirrusMilkyWayPixelationResolution, 1.0); }

float pixelateCubeCoordinate(float value) {
    float normalized = clamp(value * 0.5 + 0.5, 0.0, 1.0);
    float cell = min(
        floor(normalized * cirrusMilkyWayPixelsPerFace()),
        cirrusMilkyWayPixelsPerFace() - 1.0
    );
    return (
        (cell + 0.5) / cirrusMilkyWayPixelsPerFace()
    ) * 2.0 - 1.0;
}

vec3 pixelateDirection(vec3 direction) {
    vec3 absoluteDirection = abs(direction);
    vec3 pixelDirection;
    if (absoluteDirection.x >= absoluteDirection.y
            && absoluteDirection.x >= absoluteDirection.z) {
        pixelDirection = vec3(
            sign(direction.x),
            pixelateCubeCoordinate(direction.y / absoluteDirection.x),
            pixelateCubeCoordinate(direction.z / absoluteDirection.x)
        );
    } else if (absoluteDirection.y >= absoluteDirection.z) {
        pixelDirection = vec3(
            pixelateCubeCoordinate(direction.x / absoluteDirection.y),
            sign(direction.y),
            pixelateCubeCoordinate(direction.z / absoluteDirection.y)
        );
    } else {
        pixelDirection = vec3(
            pixelateCubeCoordinate(direction.x / absoluteDirection.z),
            pixelateCubeCoordinate(direction.y / absoluteDirection.z),
            sign(direction.z)
        );
    }
    return normalize(pixelDirection);
}

vec3 rotateX(vec3 direction, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return vec3(
        direction.x,
        direction.y * cosine - direction.z * sine,
        direction.y * sine + direction.z * cosine
    );
}

vec3 rotateY(vec3 direction, float angle) {
    float sine = sin(angle);
    float cosine = cos(angle);
    return vec3(
        direction.x * cosine + direction.z * sine,
        direction.y,
        -direction.x * sine + direction.z * cosine
    );
}

vec2 milkyWayTextureCoordinate(vec3 direction) {
    return vec2(
        atan(direction.z, direction.x) / CIRRUS_TWO_PI + 0.5,
        asin(clamp(direction.y, -1.0, 1.0)) / CIRRUS_PI + 0.5
    );
}

void main() {
    vec3 viewDirection = normalize(worldDirection);
    if (CirrusMilkyWayIntensity < 0.001) {
        float elevation = clamp(viewDirection.y, 0.0, 1.0);
        vec3 skyGradientColor = mix(
            CirrusSkyHorizonColor,
            CirrusSkyZenithColor,
            smoothstep(0.015, max(CirrusSkyGradientHeight, 0.02), elevation)
        );
        float horizonColorBoost = 1.0 - smoothstep(0.02, 0.32, elevation);
        float skyDomeFade = smoothstep(-0.18, 0.02, viewDirection.y);
        float skyGradientAlpha = CirrusSkyGradientIntensity
                * mix(0.22, 0.38, horizonColorBoost)
                * skyDomeFade;
        if (skyGradientAlpha < 0.001) {
            discard;
        }
        fragColor = vec4(skyGradientColor, skyGradientAlpha);
        return;
    }

    vec3 smoothCelestialDirection = normalize(rotateY(
        rotateX(rotateY(viewDirection, CIRRUS_QUARTER_TURN), -CirrusMilkyWayRotation),
        CIRRUS_QUARTER_TURN
    ));
    vec3 celestialDirection = mix(
        smoothCelestialDirection,
        pixelateDirection(smoothCelestialDirection),
        clamp(CirrusMilkyWayPixelation, 0.0, 1.0)
    );

    vec4 milkyWaySample = texture(
        Sampler0,
        milkyWayTextureCoordinate(celestialDirection)
    );
    float luminosity = milkyWaySample.a
            * milkyWaySample.a
            * CIRRUS_LUMINOSITY_RANGE;

    float horizonFade = smoothstep(-0.22, 0.015, viewDirection.y);
    float milkyWayAlpha = clamp(
        luminosity * horizonFade * CirrusMilkyWayIntensity * 1.18,
        0.0,
        0.64
    );

    // Blend the two configured palette stops by elevation. Java cross-fades
    // both colors and their strength between the four time-of-day phases.
    float elevation = clamp(viewDirection.y, 0.0, 1.0);
    vec3 skyGradientColor = mix(
        CirrusSkyHorizonColor,
        CirrusSkyZenithColor,
        smoothstep(0.015, max(CirrusSkyGradientHeight, 0.02), elevation)
    );
    float horizonColorBoost = 1.0 - smoothstep(0.02, 0.32, elevation);
    float skyDomeFade = smoothstep(-0.18, 0.02, viewDirection.y);
    float skyGradientAlpha = CirrusSkyGradientIntensity
            * mix(0.22, 0.38, horizonColorBoost)
            * skyDomeFade;

    // Composite the Milky Way over the configured gradient, then let Minecraft
    // blend the combined layer over its vanilla sky.
    float combinedAlpha = milkyWayAlpha
            + skyGradientAlpha * (1.0 - milkyWayAlpha);
    if (combinedAlpha < 0.001) {
        discard;
    }
    vec3 combinedColor = (
        milkyWaySample.rgb * milkyWayAlpha
        + skyGradientColor * skyGradientAlpha * (1.0 - milkyWayAlpha)
    ) / combinedAlpha;
    fragColor = vec4(combinedColor, combinedAlpha);
}
