#version 330

layout(std140) uniform CirrusParams {
    vec4 ColorModulator;
    float CirrusRainCloudCoverage;
    float CirrusThunderCloudCoverage;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in float vertexAlpha;
out vec4 fragColor;

void main() {
    float rainCoverage = clamp(CirrusRainCloudCoverage, 0.0, 1.0);
    float thunderCoverage = clamp(CirrusThunderCloudCoverage, 0.0, 1.0);
    vec2 texelSize = 1.0 / vec2(textureSize(Sampler0, 0));
    float baseCloudAlpha = texture(Sampler0, texCoord0).a;
    float rainCloudAlpha = 0.0;
    float thunderCloudAlpha = 0.0;
    if (rainCoverage > 0.001) {
        rainCloudAlpha = texture(Sampler0, texCoord0 + vec2(83.0, 47.0) * texelSize).a * rainCoverage;
    }
    if (thunderCoverage > 0.001) {
        thunderCloudAlpha = texture(Sampler0, texCoord0 + vec2(157.0, 109.0) * texelSize).a * thunderCoverage;
    }
    float supplementalCloudAlpha =
        1.0 - (1.0 - rainCloudAlpha) * (1.0 - thunderCloudAlpha);
    float cloudAlpha = 1.0 - (1.0 - baseCloudAlpha) * (1.0 - supplementalCloudAlpha);
    if (cloudAlpha * vertexAlpha * ColorModulator.a < 0.1) {
        discard;
    }
    fragColor = vec4(0.0);
}
