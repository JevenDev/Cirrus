#version 150

uniform sampler2D Sampler0;
uniform float CirrusRainCloudCoverage;
uniform float CirrusThunderCloudCoverage;
uniform float CirrusLayerOpacity;

in vec2 texCoord0;

out vec4 fragColor;

const vec2 RAIN_CLOUD_PATTERN_OFFSET = vec2(83.0, 47.0);
const vec2 THUNDER_CLOUD_PATTERN_OFFSET = vec2(157.0, 109.0);

void main() {
    vec2 texelSize = 1.0 / vec2(textureSize(Sampler0, 0));
    float rainCoverage = clamp(CirrusRainCloudCoverage, 0.0, 1.0);
    float thunderCoverage = clamp(CirrusThunderCloudCoverage, 0.0, 1.0);
    float baseAlpha = texture(Sampler0, texCoord0).a;
    float rainAlpha = rainCoverage > 0.001
        ? texture(Sampler0, texCoord0 + RAIN_CLOUD_PATTERN_OFFSET * texelSize).a * rainCoverage
        : 0.0;
    float thunderAlpha = thunderCoverage > 0.001
        ? texture(Sampler0, texCoord0 + THUNDER_CLOUD_PATTERN_OFFSET * texelSize).a * thunderCoverage
        : 0.0;
    float cloudAlpha =
        1.0 - (1.0 - baseAlpha) * (1.0 - rainAlpha) * (1.0 - thunderAlpha);
    if (cloudAlpha * CirrusLayerOpacity < 0.1) {
        discard;
    }
    fragColor = vec4(0.0);
}
