#version 150

uniform float CirrusAuroraTime;
uniform float CirrusAuroraIntensity;
uniform vec4 CirrusAuroraVariant;
uniform vec4 CirrusAuroraSettings;

in vec3 worldDirection;

out vec4 fragColor;

const float HALF_PI = 1.57079632679;

float randomValue(vec2 position) {
    return fract(sin(dot(position, vec2(127.1, 311.7))) * 43758.5453123);
}

float valueNoise(vec2 position) {
    vec2 cell = floor(position);
    vec2 local = fract(position);
    local = local * local * (3.0 - 2.0 * local);

    float bottomLeft = randomValue(cell);
    float bottomRight = randomValue(cell + vec2(1.0, 0.0));
    float topLeft = randomValue(cell + vec2(0.0, 1.0));
    float topRight = randomValue(cell + vec2(1.0, 1.0));
    return mix(
        mix(bottomLeft, bottomRight, local.x),
        mix(topLeft, topRight, local.x),
        local.y
    );
}

float evolvingNoise(vec2 position, float evolution, float seed) {
    float frame = floor(evolution);
    float blend = fract(evolution);
    blend = blend * blend * (3.0 - 2.0 * blend);
    vec2 frameStep = vec2(13.71, 31.17);
    vec2 firstOffset = frameStep * frame + vec2(seed * 7.3, seed * 11.9);
    vec2 secondOffset = firstOffset + frameStep;
    return mix(
        valueNoise(position + firstOffset),
        valueNoise(position + secondOffset),
        blend
    );
}

float circularEvolvingNoise(
    float azimuth,
    float elevation,
    float angularScale,
    float verticalScale,
    float evolution,
    float seed
) {
    vec2 circle = vec2(cos(azimuth), sin(azimuth)) * angularScale;
    vec2 verticalOffset = vec2(0.613, 1.173) * elevation * verticalScale;
    return evolvingNoise(circle + verticalOffset, evolution, seed);
}

float harmonicWave(
    float azimuth,
    float phase,
    float firstHarmonic,
    float secondHarmonic,
    float blend
) {
    return mix(
        sin(azimuth * firstHarmonic + phase),
        sin(azimuth * secondHarmonic + phase),
        blend
    );
}

void main() {
    vec3 direction = normalize(worldDirection);
    float horizonFade = smoothstep(0.015, 0.16, direction.y);
    if (horizonFade <= 0.0 || CirrusAuroraIntensity <= 0.0) {
        discard;
    }

    // Minecraft north is negative Z. Expressing the pattern in azimuth and
    // elevation keeps the light fixed in the world while the camera turns.
    float azimuth = atan(direction.x, -direction.z);
    float elevation = asin(clamp(direction.y, -1.0, 1.0));
    float time = CirrusAuroraTime;
    float movement = max(CirrusAuroraSettings.x, 0.0);
    float configuredWidth = max(CirrusAuroraSettings.y, 0.01);
    float motionAmplitude = mix(0.35, 1.18, clamp(movement / 1.25, 0.0, 1.0));

    // The renderer chooses a stable variant every Minecraft day while the
    // aurora is hidden. These values alter the actual nightly layout.
    float heightOffset = mix(-0.055, 0.070, CirrusAuroraVariant.x)
            + CirrusAuroraSettings.z;
    float widthScale = mix(0.80, 1.28, CirrusAuroraVariant.y) * configuredWidth;
    float detailScale = mix(0.84, 1.24, CirrusAuroraVariant.z);
    float sampleAzimuth = azimuth;
    float morphClock = time * 0.026 * movement;
    float slowMorph = circularEvolvingNoise(
        sampleAzimuth,
        elevation,
        1.35,
        1.7,
        morphClock,
        CirrusAuroraVariant.x * 9.0 + CirrusAuroraVariant.y
    );

    // Blend between standing wave shapes. Their phases rock back and forth
    // instead of advancing in one direction around the skybox.
    float firstPhase = CirrusAuroraVariant.w * 6.2831853
            + sin(time * 0.038 * movement + CirrusAuroraVariant.x * 4.0)
                    * 0.52 * movement;
    float secondPhase = CirrusAuroraVariant.x * 5.1
            - sin(time * 0.028 * movement + CirrusAuroraVariant.z * 5.0)
                    * 0.38 * movement;
    float firstWave = harmonicWave(
        sampleAzimuth, firstPhase, 2.0, 3.0, CirrusAuroraVariant.z
    ) * 0.064;
    float secondWave = harmonicWave(
        sampleAzimuth, secondPhase, 4.0, 6.0, CirrusAuroraVariant.y
    ) * 0.038;
    float waveBlend = 0.5 + 0.5 * sin(
        time * 0.022 * movement + CirrusAuroraVariant.y * 6.2831853
    );
    float broadWave = mix(firstWave, secondWave, waveBlend) * motionAmplitude;
    broadWave += (slowMorph - 0.5) * 0.115 * motionAmplitude;

    float lowerCenter = 0.34 + heightOffset + broadWave;
    float ribbonGap = mix(0.17, 0.29, CirrusAuroraVariant.z);
    float upperPhase = CirrusAuroraVariant.y * 4.7
            + sin(time * 0.032 * movement + CirrusAuroraVariant.w * 3.0)
                    * 0.72 * movement;
    float upperFold = harmonicWave(
        sampleAzimuth, upperPhase, 3.0, 4.0, CirrusAuroraVariant.y
    );
    float upperCenter = lowerCenter + ribbonGap
            + upperFold * 0.046 * motionAmplitude;
    float highPhase = CirrusAuroraVariant.z * 5.8
            - sin(time * 0.024 * movement + CirrusAuroraVariant.x * 4.0)
                    * 0.64 * movement;
    float highFold = harmonicWave(
        sampleAzimuth, highPhase, 2.0, 3.0, CirrusAuroraVariant.z
    );
    float highCenter = upperCenter + mix(0.18, 0.30, CirrusAuroraVariant.x)
            + highFold * 0.038 * motionAmplitude;
    float splitMorph = circularEvolvingNoise(
        sampleAzimuth,
        elevation,
        2.6,
        1.2,
        morphClock * 0.73,
        CirrusAuroraVariant.z * 13.0
    );
    float splitCenter = lowerCenter
            - mix(0.075, 0.16, CirrusAuroraVariant.z)
            + (splitMorph - 0.5) * 0.052 * motionAmplitude;

    float breathing = 1.0 + 0.11 * movement * sin(
        time * 0.026 + CirrusAuroraVariant.y * 6.2831853
    );
    float lowerRibbonOffset = (elevation - lowerCenter)
            / (0.052 * widthScale * breathing);
    float upperRibbonOffset = (elevation - upperCenter)
            / (0.082 * widthScale * mix(0.88, 1.16, CirrusAuroraVariant.z) * breathing);
    float highRibbonOffset = (elevation - highCenter)
            / (0.105 * widthScale * mix(0.82, 1.18, CirrusAuroraVariant.y));
    float splitRibbonOffset = (elevation - splitCenter)
            / (0.042 * widthScale);
    float lowerRibbon = exp(-lowerRibbonOffset * lowerRibbonOffset);
    float upperRibbon = exp(-upperRibbonOffset * upperRibbonOffset);
    float highRibbon = exp(-highRibbonOffset * highRibbonOffset);
    float splitRibbon = exp(-splitRibbonOffset * splitRibbonOffset);

    float rayNoise = circularEvolvingNoise(
        sampleAzimuth,
        elevation,
        mix(14.0, 22.0, CirrusAuroraVariant.z),
        2.4,
        morphClock * 1.65,
        CirrusAuroraVariant.x * 19.0
    );
    rayNoise *= circularEvolvingNoise(
        sampleAzimuth,
        elevation,
        7.0 * detailScale,
        4.1,
        morphClock * 1.17,
        CirrusAuroraVariant.w * 13.0
    ) * 0.75 + 0.25;
    float rays = pow(smoothstep(0.25, 0.86, rayNoise), 1.38);

    float curtainReach = mix(0.68, 0.98, CirrusAuroraVariant.y)
            + sin(time * 0.018 * movement
                    + CirrusAuroraVariant.z * 6.2831853) * 0.07 * movement;
    float curtainHeight = smoothstep(lowerCenter - 0.025, lowerCenter + 0.035, elevation)
            * (1.0 - smoothstep(
                lowerCenter + 0.12,
                min(lowerCenter + curtainReach, HALF_PI),
                elevation
            ));
    float fineShimmer = 0.70 + 0.30 * circularEvolvingNoise(
        sampleAzimuth,
        elevation,
        mix(21.0, 29.0, CirrusAuroraVariant.x),
        5.0,
        morphClock * 1.42,
        CirrusAuroraVariant.y * 17.0
    );

    float northFocus = 1.0 - smoothstep(
        mix(1.32, 1.56, CirrusAuroraVariant.y),
        mix(2.52, 2.80, CirrusAuroraVariant.z),
        abs(azimuth)
    );
    float southDistance = abs(abs(azimuth) - 3.14159265359);
    float southFocus = 1.0 - smoothstep(
        mix(0.72, 0.98, CirrusAuroraVariant.x),
        mix(1.48, 1.78, CirrusAuroraVariant.w),
        southDistance
    );
    // Keep the strongest curtain in the north, but wrap dimmer structures
    // around the full dome and give the opposite horizon its own broad lobe.
    float backgroundCoverage = mix(0.24, 0.38, CirrusAuroraVariant.y);
    float skyCoverage = max(
        mix(backgroundCoverage, 1.0, northFocus),
        southFocus * mix(0.52, 0.76, CirrusAuroraVariant.z)
    );
    float zenithFade = 1.0 - smoothstep(1.30, HALF_PI, elevation);
    float lowerStrength = mix(0.72, 1.20, CirrusAuroraVariant.x);
    float upperStrength = mix(0.34, 0.86, CirrusAuroraVariant.y);
    float highStrength = mix(0.02, 0.48, CirrusAuroraVariant.w);
    float splitStrength = smoothstep(0.42, 0.82, CirrusAuroraVariant.x) * 0.46;
    float shape = (
        lowerRibbon * lowerStrength
        + upperRibbon * upperStrength
        + highRibbon * highStrength
        + splitRibbon * splitStrength
        + curtainHeight * rays * mix(0.42, 0.60, CirrusAuroraVariant.x)
    ) * fineShimmer
            * skyCoverage
            * horizonFade
            * zenithFade;

    float heightMix = smoothstep(lowerCenter - 0.04, lowerCenter + 0.62, elevation);
    float paletteCycle = 0.5 + 0.5 * sin(
        time * 0.035 + CirrusAuroraVariant.w * 6.2831853
    );
    float localPalette = circularEvolvingNoise(
        sampleAzimuth,
        elevation,
        0.85,
        1.6,
        morphClock * 0.34,
        CirrusAuroraVariant.z * 7.0
    );
    float paletteBlend = clamp(paletteCycle * 0.72 + localPalette * 0.28, 0.0, 1.0);

    vec3 green = mix(
        vec3(0.10, 1.0, 0.38),
        vec3(0.06, 0.88, 0.68),
        paletteBlend * 0.72
    );
    vec3 cyan = mix(
        vec3(0.07, 0.76, 0.92),
        vec3(0.24, 0.52, 1.0),
        paletteBlend * 0.68
    );
    vec3 violet = mix(
        vec3(0.44, 0.34, 0.92),
        vec3(0.74, 0.20, 0.96),
        paletteBlend
    );
    vec3 color = mix(green, cyan, heightMix);
    color = mix(
        color,
        violet,
        smoothstep(0.92, 1.34, elevation)
                * mix(0.30, 0.66, paletteBlend)
                * mix(0.72, 1.0, CirrusAuroraVariant.x)
    );

    float pulseAmount = 0.10 * min(movement, 1.5);
    float pulsePhase = time * 0.060 * movement
            + CirrusAuroraVariant.w * 6.2831853;
    float pulseWave = harmonicWave(
        sampleAzimuth, pulsePhase, 1.0, 2.0, CirrusAuroraVariant.x
    );
    float pulse = 1.0 - pulseAmount + pulseAmount * pulseWave;
    float nightlyActivity = mix(0.88, 1.08, CirrusAuroraVariant.z);
    float alpha = clamp(
        shape * CirrusAuroraIntensity * pulse * nightlyActivity * 0.70,
        0.0,
        0.82
    );
    if (alpha < 0.001) {
        discard;
    }
    fragColor = vec4(color, alpha);
}
