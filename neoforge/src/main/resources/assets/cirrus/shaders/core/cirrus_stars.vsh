#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 CirrusStarAnimation;
uniform float CirrusNorthStar;

out vec2 starCoordinate;
flat out vec4 starData;
flat out float starSelection;

float starHash(vec3 position) {
    return fract(sin(dot(position, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
}

void main() {
    vec3 direction = normalize(Position);
    vec3 tangent = normalize(Normal);
    vec3 bitangent = normalize(cross(tangent, direction));
    float sizeRandom = pow(Color.g, 2.2);
    float sizeMultiplier = mix(
        CirrusStarAnimation.x,
        CirrusStarAnimation.y,
        sizeRandom
    );
    float pulseWave = 0.5 + 0.5 * sin(
        CirrusStarAnimation.z * CirrusStarAnimation.w * 1.30
    );
    float northSizePulse = 0.94 + pulseWave * 0.12;
    float halfSize = 0.20
            * sizeMultiplier
            * mix(1.0, 2.35 * northSizePulse, CirrusNorthStar);
    vec3 expandedPosition = Position
            + tangent * UV0.x * halfSize
            + bitangent * UV0.y * halfSize;

    gl_Position = ProjMat * ModelViewMat * vec4(expandedPosition, 1.0);
    starCoordinate = UV0;
    starData = Color;
    starSelection = starHash(Position);
}
