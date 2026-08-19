#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    vec4 CirrusStarAppearance;
    vec4 CirrusStarAnimation;
    vec2 CirrusStarAtmosphere;
    vec4 CirrusShootingStarAppearance;
    vec4 CirrusShootingStarAnimation;
    vec2 CirrusShootingStarDynamics;
    vec4 CirrusShootingStarVisual;
    float CirrusStarRenderMode;
};


uniform sampler2D Sampler0;


in vec2 starCoordinate;
flat in vec4 starData;
flat in float starSelection;
flat in float shootingActive;
flat in float shootingProgress;

out vec4 fragColor;

const float TWO_PI = 6.28318530718;

void main() {
    float northAmount = step(0.5, CirrusStarRenderMode) * (1.0 - step(1.5, CirrusStarRenderMode));
    float shootingAmount = step(1.5, CirrusStarRenderMode);
    if (shootingAmount > 0.5) {
        if (shootingActive < 0.5) {
            discard;
        }

        float alongTrail = clamp(starCoordinate.x, 0.0, 1.0);
        float acrossTrail = abs(starCoordinate.y);
        float trailAngle = max(0.12 * CirrusShootingStarVisual.x, 0.001);
        float trailGrowth = clamp(shootingProgress * 0.62 / trailAngle, 0.0, 1.0);
        float revealStart = 1.0 - trailGrowth;
        float revealWidth = max(min(0.08, trailGrowth * 0.35), 0.002);
        float trailReveal = smoothstep(revealStart, revealStart + revealWidth, alongTrail);
        float tailFade = smoothstep(0.0, 0.075, alongTrail) * pow(alongTrail, 0.86);
        float trailEnvelope = trailReveal * tailFade;

        float smoothCore = (1.0 - smoothstep(0.08, 0.24, acrossTrail))
                * trailEnvelope;
        float smoothHalo = (1.0 - smoothstep(0.12, 1.0, acrossTrail))
                * trailEnvelope;

        float pixelatedTrail = step(0.5, CirrusShootingStarVisual.w);
        float trailCellCount = 20.0;
        float trailCell = floor(alongTrail * trailCellCount);
        float cellCenter = (trailCell + 0.5) / trailCellCount;
        float withinCell = abs(fract(alongTrail * trailCellCount) - 0.5) * 2.0;
        float cellGap = 1.0 - smoothstep(0.76, 0.96, withinCell);
        float cellWidth = mix(0.15, 0.34, floor(cellCenter * 5.0) / 5.0);
        float pixelCore = (1.0 - smoothstep(cellWidth, cellWidth + 0.055, acrossTrail))
                * cellGap
                * trailReveal
                * smoothstep(0.0, 0.10, cellCenter)
                * pow(cellCenter, 0.78);
        float pixelHalo = (1.0 - smoothstep(cellWidth + 0.08, 0.95, acrossTrail))
                * mix(0.38, 1.0, cellGap)
                * trailReveal
                * smoothstep(0.0, 0.08, cellCenter)
                * pow(cellCenter, 0.88);

        float breakupAlong = mix(floor(alongTrail * 28.0), trailCell, pixelatedTrail);
        vec2 breakupCell = floor(vec2(breakupAlong, (starCoordinate.y + 1.0) * 4.0));
        float breakupNoise = fract(sin(
            dot(breakupCell, vec2(17.17, 91.73)) + starData.b * 37.0
        ) * 43758.5453);
        float breakupThreshold = 0.10 + breakupNoise * 0.62 + alongTrail * 0.18;
        float burnout = smoothstep(0.62, 0.995, shootingProgress);
        float fragmentSurvival = 1.0 - smoothstep(
            breakupThreshold,
            breakupThreshold + 0.10,
            burnout
        );
        float burnFlare = smoothstep(0.46, 0.68, shootingProgress)
                * (1.0 - smoothstep(0.80, 0.94, shootingProgress));
        float emberFlicker = mix(1.0, 0.76 + breakupNoise * 0.24, burnout);

        vec2 headCoordinate = vec2(
            (1.0 - alongTrail) * 6.4,
            starCoordinate.y * 0.94
        );
        float headDistance = length(headCoordinate);
        float headCore = 1.0 - smoothstep(0.06, 0.34, headDistance);
        float headHalo = 1.0 - smoothstep(0.08, 1.30, headDistance);
        float trailCore = mix(smoothCore, pixelCore, pixelatedTrail);
        float trailHalo = mix(smoothHalo, pixelHalo, pixelatedTrail);
        float core = max(trailCore, headCore * trailReveal);
        float halo = max(trailHalo * 0.74, headHalo * trailReveal);
        float bloom = CirrusShootingStarVisual.y;
        float shapeAlpha = core * (0.92 + bloom * 0.04)
                + halo * (0.11 + bloom * 0.17);
        float ignition = smoothstep(0.0, 0.018, shootingProgress);
        float alpha = shapeAlpha
                * ignition
                * fragmentSurvival
                * emberFlicker
                * (1.0 + burnFlare * 0.55)
                * CirrusShootingStarAppearance.w
                * CirrusStarAtmosphere.x;
        if (alpha < 0.001) {
            discard;
        }

        vec3 neutralGlow = vec3(0.79, 0.88, 1.0);
        vec3 warmGlow = vec3(1.0, 0.58, 0.22);
        vec3 coolGlow = vec3(0.34, 0.64, 1.0);
        float temperature = (starData.b * 2.0 - 1.0)
                * CirrusShootingStarVisual.z;
        vec3 glowColor = temperature < 0.0
                ? mix(neutralGlow, warmGlow, -temperature)
                : mix(neutralGlow, coolGlow, temperature);
        float hotCenter = clamp(core / (core + halo * 0.82 + 0.001), 0.0, 1.0);
        float headHeat = (1.0 - smoothstep(0.0, 0.85, headDistance)) * trailReveal;
        float whiteHeat = max(hotCenter * 0.82, headHeat);
        vec3 color = mix(glowColor, vec3(1.0, 0.99, 0.95), whiteHeat);
        float emberAmount = burnout * (1.0 - hotCenter * 0.72);
        color = mix(color, vec3(1.0, 0.29, 0.045), emberAmount * 0.78);
        color *= 0.94 + min(bloom, 2.0) * 0.09 + burnFlare * 0.12;
        fragColor = vec4(color, min(alpha, 1.0));
        return;
    }

    if (northAmount < 0.5 && starSelection > CirrusStarAppearance.x) {
        discard;
    }

    float squareDistance = max(abs(starCoordinate.x), abs(starCoordinate.y));
    vec4 northTexture = vec4(1.0);
    float shape = 1.0;
    if (northAmount > 0.5) {
        vec2 northTextureCoordinate = starCoordinate * 0.5 + 0.5;
        northTexture = texture(Sampler0, northTextureCoordinate);
        shape = northTexture.a;
        if (shape <= 0.001) {
            discard;
        }
    } else if (squareDistance > 1.0) {
        discard;
    }

    float minimumOpacity = CirrusStarAppearance.y;
    float maximumOpacity = CirrusStarAppearance.z;
    float opacity = mix(
        minimumOpacity,
        maximumOpacity,
        pow(starData.r, 1.65)
    );

    float twinkleFrequency = mix(0.72, 1.48, starSelection);
    float twinkleWave = 0.5 + 0.5 * sin(
        CirrusStarAnimation.z
                * CirrusStarAnimation.w
                * twinkleFrequency
                + starData.a * TWO_PI
    );
    float twinkle = mix(
        1.0,
        0.72 + twinkleWave * 0.28,
        CirrusStarAppearance.w
    );

    vec3 neutral = vec3(0.93, 0.965, 1.0);
    vec3 warm = vec3(1.0, 0.82, 0.67);
    vec3 cool = vec3(0.66, 0.80, 1.0);
    float temperature = starData.b * 2.0 - 1.0;
    vec3 tinted = temperature < 0.0
            ? mix(neutral, warm, -temperature)
            : mix(neutral, cool, temperature);
    vec3 color = mix(neutral, tinted, CirrusStarAtmosphere.y);
    color = mix(color, northTexture.rgb, northAmount);

    float pulseWave = 0.5 + 0.5 * sin(
        CirrusStarAnimation.z * CirrusStarAnimation.w * 1.30
    );
    float northBrightnessPulse = mix(
        1.0,
        0.88 + pulseWave * 0.24,
        northAmount
    );
    float northBrightness = mix(1.0, 1.75, northAmount);
    float alpha = min(
        shape
                * opacity
                * twinkle
                * northBrightnessPulse
                * northBrightness
                * CirrusStarAtmosphere.x,
        1.0
    );
    if (alpha < 0.001) {
        discard;
    }
    fragColor = vec4(color, alpha);
}
