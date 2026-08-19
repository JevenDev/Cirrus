#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    vec4 ColorModulator;
    float FogStart;
    float FogEnd;
    vec4 FogColor;
    float FogShape;
    float CirrusEnabled;
    vec2 CirrusLightDirection;
    float CirrusSunWeight;
    vec4 CirrusLightViewDirection;
    float CirrusRainLevel;
    float CirrusThunderLevel;
    float CirrusRainCloudCoverage;
    float CirrusThunderCloudCoverage;
    float CirrusLightningFlash;
    vec4 CirrusLightningViewPosition;
    vec4 CirrusWorldUpViewDirection;
    float CirrusLightningRadius;
};


in vec3 Position;
in vec2 UV0;
in vec4 Color;


out vec2 texCoord0;
out float vertexDistance;
out vec4 vertexColor;
out vec3 viewDirection;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * pos;

    texCoord0 = UV0;
    vertexDistance = max(length(pos.xz), abs(pos.y));
    vertexColor = Color;
    viewDirection = pos.xyz;
}
