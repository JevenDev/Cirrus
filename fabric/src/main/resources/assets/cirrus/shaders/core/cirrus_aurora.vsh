#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    float CirrusAuroraTime;
    float CirrusAuroraIntensity;
    float CirrusAuroraPixelation;
    float CirrusAuroraPixelationResolution;
    vec4 CirrusAuroraVariant;
    vec4 CirrusAuroraSettings;
};


in vec3 Position;


out vec3 worldDirection;

void main() {
    vec4 clipPosition = ProjMat * ModelViewMat * vec4(Position, 1.0);
    clipPosition.z = clipPosition.w;
    gl_Position = clipPosition;
    worldDirection = normalize(Position);
}
