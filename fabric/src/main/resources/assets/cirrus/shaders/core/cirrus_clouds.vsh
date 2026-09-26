#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    vec4 ColorModulator;
    float CirrusTimeOpacity;
    float CirrusEnabled;
    vec2 CirrusLightDirection;
    float CirrusSunWeight;
    vec4 CirrusLightViewDirection;
    vec4 CirrusMoonViewDirection;
    float CirrusRainLevel;
    float CirrusThunderLevel;
    float CirrusRainCloudCoverage;
    float CirrusThunderCloudCoverage;
    float CirrusLightningFlash;
    vec4 CirrusLightningViewPosition;
    vec4 CirrusWorldUpViewDirection;
    float CirrusLightningRadius;
    vec4 CirrusFogOverride;
    vec2 CirrusFogDistance;
};


in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;


out vec2 texCoord0;
out float vertexDistance;
out vec4 vertexColor;
out vec3 viewDirection;
flat out vec2 cloudNeighborOffset;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * pos;

    texCoord0 = UV0;
    vertexDistance = length(pos.xyz);
    vertexColor = Color;
    viewDirection = pos.xyz;
    // fancy side faces are one mesh cell apart, matching the renderer's UV_SCALE
    cloudNeighborOffset = Normal.xz / 256.0;
}
