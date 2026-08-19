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



in vec3 worldDirection;

out vec4 fragColor;

float hash31(vec3 point) {
    point = fract(point * 0.1031);
    point += dot(point, point.yzx + 33.33);
    return fract((point.x + point.y) * point.z);
}

float valueNoise(vec3 point) {
    vec3 cell = floor(point);
    vec3 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);

    float n000 = hash31(cell + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0, 1.0, 1.0));

    float x00 = mix(n000, n100, local.x);
    float x10 = mix(n010, n110, local.x);
    float x01 = mix(n001, n101, local.x);
    float x11 = mix(n011, n111, local.x);
    return mix(mix(x00, x10, local.y), mix(x01, x11, local.y), local.z);
}

float fbm(vec3 point) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int octave = 0; octave < 4; octave++) {
        sum += valueNoise(point) * amplitude;
        point = point * 2.03 + vec3(7.1, 3.7, 5.9);
        amplitude *= 0.5;
    }
    return sum;
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

    vec3 smoothCelestialDirection = rotateX(viewDirection, -CirrusMilkyWayRotation);
    vec3 celestialDirection = mix(
        smoothCelestialDirection,
        pixelateDirection(smoothCelestialDirection),
        clamp(CirrusMilkyWayPixelation, 0.0, 1.0)
    );

    // Build an asymmetric galactic plane. The projected core direction marks
    // the bright, wide bulge visible in real Milky Way photographs.
    vec3 galacticNormal = normalize(vec3(0.31, 0.84, 0.44));
    vec3 coreReference = vec3(0.86, -0.18, -0.48);
    vec3 coreDirection = normalize(
        coreReference - galacticNormal * dot(coreReference, galacticNormal)
    );
    float coreFacing = max(dot(celestialDirection, coreDirection), 0.0);
    float coreBulge = pow(coreFacing, 3.2);

    // Low-frequency domain warping bends the belt and prevents either edge
    // from reading as a mathematically perfect stripe.
    float warpAlong = fbm(celestialDirection * 2.15 + vec3(1.7, 9.2, 4.3));
    float warpAcross = fbm(celestialDirection * 3.65 + vec3(13.1, 2.7, 8.4));
    vec3 domainWarp = vec3(
        warpAlong - 0.47,
        warpAcross - 0.47,
        warpAlong - warpAcross
    );
    float latitude = dot(celestialDirection, galacticNormal);
    float bentLatitude = latitude
            + domainWarp.x * 0.16
            + domainWarp.y * 0.075;
    float bandDistance = abs(bentLatitude);

    float halfWidth = mix(0.145, 0.34, coreBulge);
    float outerEnvelope = exp(-pow(bandDistance / (halfWidth * 1.72), 2.0));
    float broadBand = exp(-pow(bandDistance / halfWidth, 2.0));
    float innerBand = exp(-pow(bandDistance / (halfWidth * 0.48), 2.0));

    // Domain-warped fields form large luminous clouds, finer knots, and
    // irregular negative-space rifts. None of these follow the band center.
    vec3 cloudPoint = celestialDirection * 5.4
            + domainWarp * 3.4
            + vec3(11.0, 2.0, 17.0);
    float cloudField = fbm(cloudPoint);
    float fineField = fbm(
        celestialDirection * 11.5
                + domainWarp * 5.2
                + vec3(3.0, 19.0, 7.0)
    );
    float dustField = fbm(
        celestialDirection * 8.2
                + domainWarp * 4.6
                + vec3(23.0, 5.0, 13.0)
    );

    float billows = smoothstep(
        0.30,
        0.69,
        cloudField + (fineField - 0.47) * 0.30
    );
    float filaments = smoothstep(
        0.47,
        0.76,
        fineField + cloudField * 0.16
    );
    float luminousKnots = smoothstep(
        0.69,
        0.86,
        fineField + cloudField * 0.22
    );

    float luminosity = outerEnvelope * (0.030 + billows * 0.075)
            + broadBand * (0.075 + billows * 0.235)
            + innerBand * (0.045 + filaments * 0.16);
    luminosity += coreBulge
            * broadBand
            * (0.16 + billows * 0.34 + luminousKnots * 0.20);

    // Patchy molecular clouds subtract light in clustered shapes. Keeping the
    // mask two-dimensional avoids the black center seam from the first pass.
    float dustClouds = smoothstep(
        0.51,
        0.76,
        dustField + (1.0 - billows) * 0.10
    ) * innerBand;
    float dustStrength = mix(0.36, 0.70, coreBulge);
    luminosity *= 1.0 - dustClouds * dustStrength;

    float horizonFade = smoothstep(-0.22, 0.015, viewDirection.y);
    float milkyWayAlpha = clamp(
        luminosity * horizonFade * CirrusMilkyWayIntensity * 1.18,
        0.0,
        0.64
    );

    float warmth = coreBulge * (0.58 + billows * 0.42);
    vec3 coolHaze = vec3(0.28, 0.36, 0.76);
    vec3 lavenderCloud = vec3(0.66, 0.51, 0.80);
    vec3 roseCloud = vec3(0.95, 0.42, 0.54);
    vec3 amberCore = vec3(1.0, 0.72, 0.36);
    vec3 milkyWayColor = mix(
        coolHaze,
        lavenderCloud,
        clamp(broadBand * 0.38 + billows * 0.50 + filaments * 0.16, 0.0, 1.0)
    );
    milkyWayColor = mix(milkyWayColor, roseCloud, warmth * 0.58);
    milkyWayColor = mix(
        milkyWayColor,
        amberCore,
        warmth * (0.12 + luminousKnots * 0.30)
    );
    milkyWayColor *= 0.93 + luminousKnots * 0.15;

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
        milkyWayColor * milkyWayAlpha
        + skyGradientColor * skyGradientAlpha * (1.0 - milkyWayAlpha)
    ) / combinedAlpha;
    fragColor = vec4(combinedColor, combinedAlpha);
}
