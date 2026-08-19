#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    float CirrusEndNoiseOctaves;
    float CirrusEndTime;
    float CirrusEndIntensity;
    float CirrusEndAnimationSpeed;
    float CirrusEndMorphSpeed;
    float CirrusEndVoidCoverage;
    float CirrusEndVoidDarkness;
    float CirrusEndLightningFrequency;
    float CirrusEndLightningIntensity;
    float CirrusEndSurgeFrequency;
    float CirrusEndSurgeStrength;
    float CirrusEndPixelationResolution;
    float CirrusEndPixelation;
};


in vec3 Position;


out vec3 worldDirection;

void main() {
    vec4 clipPosition = ProjMat * ModelViewMat * vec4(Position, 1.0);
    clipPosition.z = clipPosition.w;
    gl_Position = clipPosition;
    worldDirection = normalize(Position);
}
