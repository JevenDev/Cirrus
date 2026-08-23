#version 330

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


in vec3 Position;


out vec3 worldDirection;

void main() {
    vec4 clipPosition = ProjMat * ModelViewMat * vec4(Position, 1.0);
    clipPosition.z = clipPosition.w;
    gl_Position = clipPosition;
    worldDirection = normalize(Position);
}
