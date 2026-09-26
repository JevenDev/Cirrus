#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

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
