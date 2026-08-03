#version 150

uniform sampler2D Sampler0;

uniform vec4 CirrusStarAppearance;
uniform vec4 CirrusStarAnimation;
uniform vec2 CirrusStarAtmosphere;
uniform float CirrusNorthStar;

in vec2 starCoordinate;
flat in vec4 starData;
flat in float starSelection;

out vec4 fragColor;

const float TWO_PI = 6.28318530718;

void main() {
    if (starSelection > CirrusStarAppearance.x) {
        discard;
    }

    float northAmount = step(0.5, CirrusNorthStar);
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
