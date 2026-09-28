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
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;
layout(location = 3) in vec3 Normal;


layout(location = 0) out vec3 worldDirection;
layout(location = 1) out vec3 barycentric;
layout(location = 2) flat out vec3 shardSeed;
layout(location = 3) flat out vec3 shardNormal;
layout(location = 4) flat out float shardDeparture;
layout(location = 5) flat out float beamStrength;

vec3 spin(vec3 vector, vec3 axis, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vector * c + cross(axis, vector) * s + axis * dot(axis, vector) * (1.0 - c);
}

vec3 beamRandom(vec3 seed) {
    seed = fract(seed * vec3(0.1031, 0.1030, 0.0973));
    seed += dot(seed, seed.yxz + 33.33);
    return fract((seed.xxy + seed.yzz) * seed.zyx);
}

void main() {
    vec3 center = Normal * 100.0;
    vec3 outward = normalize(Normal);
    vec3 axis = normalize(Color.rgb - 0.5 + vec3(0.001));
    float impactDistance = acos(clamp(dot(outward, CirrusEndImpact.xyz), -1.0, 1.0)) / 3.14159265;
    float delay = impactDistance * 0.22 + Color.g * 0.025;
    float departure = clamp((CirrusEndShatter - delay) / (1.0 - delay), 0.0, 1.0);
    float flightTime = departure * 2.5;
    float angle = flightTime * mix(0.7, 2.8, Color.b) * (Color.r < 0.5 ? -1.0 : 1.0);
    vec3 tangent = normalize(outward * dot(outward, CirrusEndImpact.xyz) - CirrusEndImpact.xyz + axis * 0.12);
    vec3 velocity = tangent * mix(12.0, 32.0, Color.g)
            + cross(outward, axis) * 12.0
            + outward * mix(-8.0, 18.0, Color.r)
            + vec3(0.0, mix(2.0, 12.0, Color.b), 0.0);
    vec3 displacement = velocity * flightTime - vec3(0.0, 12.0 * flightTime * flightTime, 0.0);
    float opening = smoothstep(0.35, 0.95, CirrusEndCharge) * step(0.94, Color.g);
    vec3 position = center + spin((Position - center) * (1.0 - opening * 0.035), axis, angle) + displacement;
    float stress = smoothstep(impactDistance - 0.12, impactDistance + 0.12, CirrusEndCharge * 1.12);
    float release = smoothstep(0.0, 0.025, departure)
            * (1.0 - smoothstep(0.04, 0.12, departure));
    float beamFade = 1.0 - smoothstep(0.06, 0.20, departure);
    beamStrength = step(0.94, Color.g) * stress * opening * pow(CirrusEndCharge, 1.5)
            * (0.40 + release * 0.36) * beamFade;
    if (CirrusEndBeamPass > 0.5) {
        float beamIndex = floor(UV0.y / 4.0);
        vec3 random = beamRandom(Color.rgb * 17.0 + beamIndex * vec3(7.0, 23.0, 37.0));
        float beamStart = mix(0.40, 0.74, random.b);
        float emergence = smoothstep(beamStart, min(1.0, beamStart + 0.24), CirrusEndCharge);
        float energy = stress * emergence * pow(CirrusEndCharge, 1.5);
        beamStrength = energy * (0.40 + release * 0.36) * mix(0.65, 1.0, random.r)
                * beamFade;
        vec3 surfaceTangent = normalize(cross(outward, abs(outward.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
        vec3 surfaceBitangent = cross(outward, surfaceTangent);
        float azimuth = random.g * 6.2831853;
        vec3 beamAxis = normalize(-outward * mix(0.35, 0.80, random.b)
                + surfaceTangent * cos(azimuth) + surfaceBitangent * sin(azimuth));
        float sweep = sin(CirrusEndCharge * 3.0 + random.g * 20.0) * 0.18 * CirrusEndCharge;
        beamAxis = spin(spin(beamAxis, outward, sweep), axis, angle);
        vec3 beamTangent = normalize(cross(beamAxis, abs(beamAxis.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
        vec3 beamBitangent = cross(beamAxis, beamTangent);
        float rotation = random.r * 6.2831853 + CirrusEndCharge * 1.5707963;
        float coneAngle = mod(UV0.y, 4.0) / 3.0 * 6.2831853 + rotation;
        float length = mix(50.0, 145.0, pow(random.r, 1.8)) * energy * (1.0 + release * 0.20);
        float width = mix(4.0, 11.0, random.b) * sqrt(energy) * (1.0 + release * 0.40);
        position += UV0.x * (beamAxis * length
                + (beamTangent * cos(coneAngle) + beamBitangent * sin(coneAngle)) * width);
    }
    vec4 clipPosition = ProjMat * ModelViewMat * vec4(position, 1.0);
    clipPosition.z = clipPosition.w;
    gl_Position = clipPosition;
    worldDirection = position;
    barycentric = CirrusEndBeamPass > 0.5 ? vec3(UV0.x, 0.0, 0.0)
            : vec3(UV0, 1.0 - UV0.x - UV0.y);
    shardSeed = Color.rgb;
    shardNormal = spin(outward, axis, angle);
    shardDeparture = departure;
}
