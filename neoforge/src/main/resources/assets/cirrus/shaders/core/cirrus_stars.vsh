#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 CirrusStarAnimation;
uniform vec4 CirrusShootingStarAppearance;
uniform vec4 CirrusShootingStarAnimation;
uniform vec4 CirrusShootingStarVisual;
uniform float CirrusStarRenderMode;

out vec2 starCoordinate;
flat out vec4 starData;
flat out float starSelection;
flat out float shootingActive;
flat out float shootingProgress;

float starHash(vec3 position) {
    return fract(sin(dot(position, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
}

void main() {
    vec3 direction = normalize(Position);
    vec3 tangent = normalize(Normal);
    vec3 bitangent = normalize(cross(tangent, direction));
    float sizeRandom = pow(Color.g, 2.2);
    float northAmount = step(0.5, CirrusStarRenderMode) * (1.0 - step(1.5, CirrusStarRenderMode));
    float shootingAmount = step(1.5, CirrusStarRenderMode);

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
            * mix(1.0, 2.35 * northSizePulse, northAmount);
    vec3 expandedPosition = Position
            + tangent * UV0.x * halfSize
            + bitangent * UV0.y * halfSize;

    shootingActive = 0.0;
    shootingProgress = 0.0;
    if (shootingAmount > 0.5) {
        float frequency = CirrusShootingStarAnimation.y;
        float eventInterval = 60.0 / max(frequency, 0.001);
        float eventCursor = CirrusShootingStarAnimation.x / eventInterval + 0.61803398875;
        float eventIndex = floor(eventCursor);
        float eventElapsed = fract(eventCursor) * eventInterval;
        float speedRandom = pow(Color.g, 1.35);
        float shootingSpeed = mix(
            CirrusShootingStarAnimation.z,
            CirrusShootingStarAnimation.w,
            speedRandom
        );
        float duration = 1.25 / shootingSpeed;
        shootingProgress = clamp(eventElapsed / duration, 0.0, 1.0);

        float selectedSlot = mod(eventIndex, 32.0) / 31.0;
        float slotMatch = 1.0 - step(0.5 / 31.0, abs(Color.a - selectedSlot));
        float eventActive = 1.0 - step(duration, eventElapsed);
        shootingActive = CirrusShootingStarAppearance.x
                * step(0.001, frequency)
                * slotMatch
                * eventActive;

        float shootingSize = mix(
            CirrusShootingStarAppearance.y,
            CirrusShootingStarAppearance.z,
            pow(Color.r, 1.8)
        );
        float alongTrail = clamp(UV0.x, 0.0, 1.0);
        float headAngle = shootingProgress * 0.62;
        float trailAngle = max(0.12 * CirrusShootingStarVisual.x, 0.001);
        float sampleAngle = headAngle - (1.0 - alongTrail) * trailAngle;
        vec3 pathDirection = direction * cos(sampleAngle) + tangent * sin(sampleAngle);
        vec3 travelTangent = normalize(
            -direction * sin(sampleAngle) + tangent * cos(sampleAngle)
        );
        vec3 widthTangent = normalize(cross(travelTangent, pathDirection));
        float widthTaper = mix(0.38, 1.0, pow(alongTrail, 0.55));
        float halfWidth = 0.30 * shootingSize * widthTaper;
        expandedPosition = pathDirection * length(Position)
                + widthTangent * UV0.y * halfWidth;
    }

    gl_Position = ProjMat * ModelViewMat * vec4(expandedPosition, 1.0);
    starCoordinate = UV0;
    starData = Color;
    starSelection = starHash(Position);
}
