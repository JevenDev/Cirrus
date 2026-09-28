#version 330
#extension GL_ARB_separate_shader_objects : require

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
    float CirrusEndGlass;
    float CirrusEndFlash;
    float CirrusEndShatter;
    float CirrusEndCharge;
    vec4 CirrusEndImpact;
    float CirrusEndBeamPass;
    float CirrusEndVeil;
    float CirrusEndSkyEnabled;
};

layout(location = 0) in vec3 Position;


layout(location = 0) out vec3 worldDirection;
layout(location = 1) out vec3 barycentric;
layout(location = 2) flat out vec3 shardSeed;
layout(location = 3) flat out vec3 shardNormal;
layout(location = 4) flat out float shardDeparture;
layout(location = 5) flat out float beamStrength;

void main() {
    vec4 clipPosition = ProjMat * ModelViewMat * vec4(Position, 1.0);
    clipPosition.z = clipPosition.w;
    gl_Position = clipPosition;
    worldDirection = normalize(Position);
    barycentric = vec3(1.0);
    shardSeed = vec3(0.0);
    shardNormal = vec3(0.0, 1.0, 0.0);
    shardDeparture = 0.0;
    beamStrength = 0.0;
}
