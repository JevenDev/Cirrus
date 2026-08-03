#version 150

uniform sampler2D Sampler0;

uniform vec4 CirrusStarAppearance;
uniform vec4 CirrusStarAnimation;
uniform vec2 CirrusStarAtmosphere;
uniform vec4 CirrusShootingStarAppearance;
uniform float CirrusStarRenderMode;
uniform vec4 CirrusShootingStarVisual;

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
        float trailEnvelope = trailReveal
                * smoothstep(0.0, 0.06, alongTrail)
                * pow(alongTrail, 0.82);
        float coreWidth = 1.0 - smoothstep(0.0, 0.18, acrossTrail);
        float haloWidth = 1.0 - smoothstep(0.05, 1.0, acrossTrail);
        vec2 headCoordinate = vec2(
            (1.0 - alongTrail) * 5.5,
            starCoordinate.y * 0.78
        );
        float headDistance = length(headCoordinate);
        float headCore = 1.0 - smoothstep(0.0, 0.32, headDistance);
        float headHalo = 1.0 - smoothstep(0.05, 1.25, headDistance);
        float core = max(coreWidth * pow(alongTrail, 1.05), headCore) * trailReveal;
        float halo = max(haloWidth * trailEnvelope * 0.78, headHalo * trailReveal);
        float bloom = CirrusShootingStarVisual.y;
        float shapeAlpha = core * 0.92 + halo * (0.14 + bloom * 0.20);
        float fade = smoothstep(0.0, 0.08, shootingProgress)
                * (1.0 - smoothstep(0.76, 1.0, shootingProgress));
        float alpha = shapeAlpha * fade
                * CirrusShootingStarAppearance.w
                * CirrusStarAtmosphere.x;
        if (alpha < 0.001) {
            discard;
        }

        vec3 neutralGlow = vec3(0.84, 0.91, 1.0);
        vec3 warmGlow = vec3(1.0, 0.64, 0.30);
        vec3 coolGlow = vec3(0.38, 0.68, 1.0);
        float temperature = (starData.b * 2.0 - 1.0)
                * CirrusShootingStarVisual.z;
        vec3 glowColor = temperature < 0.0
                ? mix(neutralGlow, warmGlow, -temperature)
                : mix(neutralGlow, coolGlow, temperature);
        float coreMix = clamp(core / (core + halo * 0.70 + 0.001), 0.0, 1.0);
        vec3 color = mix(glowColor, vec3(1.0, 0.985, 0.94), coreMix);
        color *= 0.95 + min(bloom, 2.0) * 0.08;
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
