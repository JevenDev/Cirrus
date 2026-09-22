#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 CirrusProjMat;
};

layout(std140) uniform CirrusParams {
    vec4 ColorModulator;
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
};


layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;


layout(location = 0) out vec2 texCoord0;
layout(location = 1) out float vertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec3 viewDirection;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = CirrusProjMat * pos;

    texCoord0 = UV0;
    vertexDistance = length(pos.xyz);
    vertexColor = Color;
    viewDirection = pos.xyz;
}
