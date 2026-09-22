#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    float CirrusRainCloudCoverage;
    float CirrusThunderCloudCoverage;
    float CirrusLayerOpacity;
};


layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;


layout(location = 0) out vec2 texCoord0;

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    vec4 clipPosition = ProjMat * viewPosition;
    clipPosition.z = clipPosition.w;
    gl_Position = clipPosition;
    texCoord0 = UV0;
}
