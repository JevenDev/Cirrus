#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    float CirrusMilkyWayIntensity;
    float CirrusMilkyWayPixelation;
    float CirrusMilkyWayPixelationResolution;
    float CirrusSkyGradientIntensity;
    float CirrusSkyGradientHeight;
    float CirrusMilkyWayRotation;
    vec3 CirrusSkyHorizonColor;
    vec3 CirrusSkyZenithColor;
};


layout(location = 0) in vec3 Position;


layout(location = 0) out vec3 worldDirection;

void main() {
    vec4 clipPosition = ProjMat * ModelViewMat * vec4(Position, 1.0);
    clipPosition.z = clipPosition.w;
    gl_Position = clipPosition;
    worldDirection = normalize(Position);
}
