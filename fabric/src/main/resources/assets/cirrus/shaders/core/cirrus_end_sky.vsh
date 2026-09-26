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
    float CirrusEndGlass;
    float CirrusEndFlash;
    float CirrusEndShatter;
    float CirrusEndCharge;
    vec4 CirrusEndImpact;
    float CirrusEndBeamPass;
    float CirrusEndVeil;
};

in vec3 Position;


out vec3 worldDirection;
out vec3 barycentric;
flat out vec3 shardSeed;
flat out vec3 shardNormal;
flat out float shardDeparture;
flat out float beamStrength;

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
