#version 150

uniform float CirrusEndTime;
uniform float CirrusEndIntensity;
uniform float CirrusEndAnimationSpeed;
uniform float CirrusEndMorphSpeed;
uniform float CirrusEndVoidCoverage;
uniform float CirrusEndVoidDarkness;
uniform float CirrusEndLightningFrequency;
uniform float CirrusEndLightningIntensity;
uniform float CirrusEndSurgeFrequency;
uniform float CirrusEndSurgeStrength;
uniform float CirrusEndPixelation;

in vec3 worldDirection;
out vec4 fragColor;

const float PI = 3.14159265359;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 hash33(vec3 p) {
    p = vec3(
        dot(p, vec3(127.1, 311.7, 74.7)),
        dot(p, vec3(269.5, 183.3, 246.1)),
        dot(p, vec3(113.5, 271.9, 124.6))
    );
    return fract(sin(p) * 43758.5453);
}

float valueNoise(vec3 p) {
    vec3 cell = floor(p);
    vec3 local = fract(p);
    local = local * local * (3.0 - 2.0 * local);

    float n000 = hash13(cell + vec3(0.0, 0.0, 0.0));
    float n100 = hash13(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash13(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash13(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash13(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash13(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash13(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash13(cell + vec3(1.0, 1.0, 1.0));

    float nearZ = mix(mix(n000, n100, local.x), mix(n010, n110, local.x), local.y);
    float farZ = mix(mix(n001, n101, local.x), mix(n011, n111, local.x), local.y);
    return mix(nearZ, farZ, local.z);
}

float fbm(vec3 p) {
    float result = 0.0;
    float weight = 0.53;
    mat3 octaveRotation = mat3(
        0.00, 0.80, 0.60,
       -0.80, 0.36, -0.48,
       -0.60, -0.48, 0.64
    );
    for (int octave = 0; octave < 4; octave++) {
        result += valueNoise(p) * weight;
        p = octaveRotation * p * 2.03 + vec3(7.1, 13.7, 19.3);
        weight *= 0.49;
    }
    return result;
}

vec3 rotateY(vec3 p, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec3(c * p.x + s * p.z, p.y, -s * p.x + c * p.z);
}

vec2 octahedralEncode(vec3 direction) {
    direction /= abs(direction.x) + abs(direction.y) + abs(direction.z);
    vec2 encoded = direction.xz;
    if (direction.y < 0.0) {
        encoded = (1.0 - abs(encoded.yx)) * sign(encoded);
    }
    return encoded;
}

vec3 octahedralDecode(vec2 encoded) {
    vec3 direction = vec3(encoded.x, 1.0 - abs(encoded.x) - abs(encoded.y), encoded.y);
    if (direction.y < 0.0) {
        direction.xz = (1.0 - abs(direction.zx)) * sign(direction.xz);
    }
    return normalize(direction);
}

vec3 pixelatedDirection(vec3 direction) {
    vec2 encoded = octahedralEncode(direction);
    vec2 resolution = vec2(480.0);
    vec2 snapped = (floor((encoded * 0.5 + 0.5) * resolution) + 0.5) / resolution;
    return octahedralDecode(snapped * 2.0 - 1.0);
}

float starLayer(vec3 direction, float scale, float threshold) {
    vec3 samplePosition = direction * scale;
    vec3 cell = floor(samplePosition);
    vec3 local = fract(samplePosition) - 0.5;
    vec3 random = hash33(cell);
    vec3 starPosition = (random - 0.5) * 0.72;
    float distanceToStar = length(local - starPosition);
    float exists = step(threshold, hash13(cell + 91.7));
    float core = 1.0 - smoothstep(0.018, 0.072, distanceToStar);
    float twinkle = 0.82 + 0.18 * sin(CirrusEndTime * 0.55 + random.x * 41.0);
    return exists * core * twinkle;
}

float shardLayer(vec3 direction) {
    vec3 samplePosition = direction * 26.0;
    vec3 cell = floor(samplePosition);
    vec3 local = fract(samplePosition) - 0.5;
    vec3 random = hash33(cell + 17.0);
    local -= (random - 0.5) * 0.62;
    float exists = step(0.987, hash13(cell + 53.0));
    float body = 1.0 - smoothstep(0.035, 0.075, max(abs(local.x) * 0.34, abs(local.y) + abs(local.z) * 0.62));
    float halo = 1.0 - smoothstep(0.055, 0.18, length(local * vec3(0.45, 1.0, 1.0)));
    return exists * (body + halo * 0.25);
}

float lightningSegment(vec2 uv, vec2 startPoint, vec2 endPoint, float seed, float width) {
    vec2 segment = endPoint - startPoint;
    float segmentLengthSquared = max(dot(segment, segment), 0.000001);
    float progress = clamp(dot(uv - startPoint, segment) / segmentLengthSquared, 0.0, 1.0);
    vec2 normal = normalize(vec2(-segment.y, segment.x));

    float coarseCell = floor(progress * 9.0);
    float coarseLocal = fract(progress * 9.0);
    float coarseA = hash13(vec3(coarseCell, seed * 17.13, seed + 4.0)) - 0.5;
    float coarseB = hash13(vec3(coarseCell + 1.0, seed * 17.13, seed + 4.0)) - 0.5;
    float crooked = mix(coarseA, coarseB, coarseLocal) * width * 7.0;

    float fineCell = floor(progress * 23.0);
    float fineLocal = fract(progress * 23.0);
    float fineA = hash13(vec3(fineCell, seed * 9.71, seed + 19.0)) - 0.5;
    float fineB = hash13(vec3(fineCell + 1.0, seed * 9.71, seed + 19.0)) - 0.5;
    crooked += mix(fineA, fineB, fineLocal) * width * 2.2;
    crooked *= sin(progress * PI);

    vec2 closest = mix(startPoint, endPoint, progress) + normal * crooked;
    float distanceToSegment = length(uv - closest);
    float core = 1.0 - smoothstep(width * 0.18, width * 0.72, distanceToSegment);
    float glow = 1.0 - smoothstep(width * 0.72, width * 3.0, distanceToSegment);
    return core + glow * 0.14;
}

float chainLightning(vec3 direction, vec3 axis, float seed) {
    vec3 tangent = normalize(cross(axis, abs(axis.y) > 0.8 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0)));
    vec3 bitangent = cross(axis, tangent);
    float facing = dot(direction, axis);
    vec2 uv = vec2(dot(direction, tangent), dot(direction, bitangent)) / max(facing, 0.24);
    vec3 bends = hash33(vec3(seed * 31.7 + 2.0, seed * 13.1 + 7.0, seed * 47.3 + 11.0)) - 0.5;

    vec2 point0 = vec2(bends.z * 0.035, 0.36);
    vec2 point1 = vec2(bends.x * 0.105, 0.19);
    vec2 point2 = vec2(bends.y * 0.090, 0.015);
    vec2 point3 = vec2(-bends.x * 0.080, -0.17);
    vec2 point4 = vec2(bends.z * 0.060, -0.35);

    float bolt = lightningSegment(uv, point0, point1, seed + 1.0, 0.0048);
    bolt += lightningSegment(uv, point1, point2, seed + 2.0, 0.0043);
    bolt += lightningSegment(uv, point2, point3, seed + 3.0, 0.0038);
    bolt += lightningSegment(uv, point3, point4, seed + 4.0, 0.0032);

    vec2 branchA = point1 + vec2(-0.13 - bends.y * 0.05, -0.13);
    vec2 branchB = point2 + vec2(0.12 + bends.z * 0.05, -0.12);
    vec2 branchC = point3 + vec2(-0.085 + bends.x * 0.04, -0.09);
    bolt += lightningSegment(uv, point1, branchA, seed + 5.0, 0.0028) * 0.62;
    bolt += lightningSegment(uv, point2, branchB, seed + 6.0, 0.0025) * 0.52;
    bolt += lightningSegment(uv, point3, branchC, seed + 7.0, 0.0021) * 0.40;
    return bolt * smoothstep(0.64, 0.88, facing);
}

float lightningBolt(vec3 direction, vec3 axis, float seed) {
    vec3 tangent = normalize(cross(axis, abs(axis.y) > 0.8 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0)));
    vec3 bitangent = cross(axis, tangent);
    float facing = dot(direction, axis);
    vec2 uv = vec2(dot(direction, tangent), dot(direction, bitangent)) / max(facing, 0.24);
    vec3 bends = hash33(vec3(seed * 23.9 + 5.0, seed * 41.3 + 17.0, seed * 11.7 + 29.0)) - 0.5;

    vec2 point0 = vec2(bends.x * 0.025, 0.24);
    vec2 point1 = vec2(bends.y * 0.060, 0.085);
    vec2 point2 = vec2(-bends.x * 0.050, -0.075);
    vec2 point3 = vec2(bends.z * 0.040, -0.23);
    float bolt = lightningSegment(uv, point0, point1, seed + 11.0, 0.0032);
    bolt += lightningSegment(uv, point1, point2, seed + 12.0, 0.0028);
    bolt += lightningSegment(uv, point2, point3, seed + 13.0, 0.0023);
    vec2 branch = point1 + vec2(0.075 + bends.z * 0.035, -0.095);
    bolt += lightningSegment(uv, point1, branch, seed + 14.0, 0.0019) * 0.44;
    return bolt * smoothstep(0.72, 0.90, facing);
}

float cloudBank(vec3 direction, float scale, vec3 drift, vec3 seed, float morphTime) {
    // The sampling domain itself changes over time, so silhouettes stretch,
    // divide, and recombine instead of translating as rigid noise textures.
    vec3 morphOffset = vec3(
        sin(morphTime + seed.x),
        cos(morphTime * 0.83 + seed.y),
        sin(morphTime * 0.67 + seed.z)
    ) * 0.82;
    float warpA = valueNoise(direction * 1.30 + seed * 0.13 + morphOffset);
    float warpB = valueNoise(direction.zxy * 1.16 + seed.yzx * 0.11 - morphOffset.yzx * 0.84);
    vec3 warp = vec3(
        warpA - 0.5,
        warpB - 0.5,
        mix(warpA, warpB, 0.37) - 0.5
    );
    vec3 morphDirection = normalize(direction + warp * 0.24);

    float foundation = fbm(morphDirection * scale + drift + seed);
    float billows = fbm(
        (morphDirection + warp.zxy * 0.08) * (scale * 2.35)
        - drift * 1.65
        + seed.yzx * 1.43
    );
    float detail = valueNoise(
        (morphDirection - warp.yzx * 0.06) * (scale * 5.2)
        + drift * 2.3
        - seed.zxy * 0.71
    );
    return foundation + (billows - 0.5) * 0.34 + (detail - 0.5) * 0.10;
}

float cloudMask(float field, float lowerEdge, float upperEdge) {
    return smoothstep(lowerEdge, upperEdge, field);
}

float cloudRim(float field, float lowerEdge, float upperEdge) {
    float density = cloudMask(field, lowerEdge, upperEdge);
    float interior = smoothstep(upperEdge - 0.01, upperEdge + 0.11, field);
    return max(density - interior, 0.0);
}

void compositeCloudLayer(
        inout vec3 color,
        float field,
        float lowerEdge,
        float upperEdge,
        vec3 shadowColor,
        vec3 bodyColor,
        vec3 edgeColor,
        float opacity,
        float intensity
) {
    float density = cloudMask(field, lowerEdge, upperEdge);
    float edge = cloudRim(field, lowerEdge, upperEdge);
    float lighting = smoothstep(lowerEdge, upperEdge + 0.09, field);
    vec3 layerColor = mix(shadowColor, bodyColor, lighting);
    float layerOpacity = density * opacity * min(intensity, 1.0);
    color = mix(color, layerColor, layerOpacity);
    color += edgeColor * edge * opacity * 0.72 * intensity;
}

void main() {
    vec3 smoothDirection = normalize(worldDirection);
    vec3 direction = mix(smoothDirection, pixelatedDirection(smoothDirection), clamp(CirrusEndPixelation, 0.0, 1.0));
    float intensity = clamp(CirrusEndIntensity, 0.0, 2.0);
    float time = CirrusEndTime * max(CirrusEndAnimationSpeed, 0.0);
    float morphTime = time * max(CirrusEndMorphSpeed, 0.0);
    vec3 driftingDirection = rotateY(direction, time * 0.0022);

    float vertical = direction.y * 0.5 + 0.5;
    vec3 color = mix(vec3(0.002, 0.0005, 0.010), vec3(0.010, 0.0015, 0.028), vertical);
    color += vec3(0.007, 0.0015, 0.018) * (1.0 - abs(direction.y));

    vec3 drift = vec3(time * 0.0038, -time * 0.0024, time * 0.0016);
    float broadCloud = fbm(driftingDirection * 2.45 + drift);
    float foldedCloud = fbm(
        rotateY(direction, -time * 0.0024) * 5.35 - drift * 1.58 + vec3(11.0, 4.0, -8.0)
    );
    float fineCloud = fbm(
        rotateY(direction, time * 0.0031) * 11.2 + drift * 2.15 + vec3(-6.0, 15.0, 3.0)
    );
    float nebula = smoothstep(0.46, 0.77, broadCloud + foldedCloud * 0.20);
    float filaments = pow(clamp(1.0 - abs(foldedCloud * 2.0 - 1.0), 0.0, 1.0), 4.0) * nebula;
    float dustLane = smoothstep(0.53, 0.74, fineCloud) * smoothstep(0.29, 0.74, broadCloud);

    vec3 violet = vec3(0.32, 0.035, 0.60);
    vec3 magenta = vec3(0.72, 0.045, 0.50);
    vec3 indigo = vec3(0.07, 0.20, 0.62);
    vec3 nebulaColor = mix(violet, magenta, smoothstep(0.39, 0.73, foldedCloud));
    nebulaColor = mix(nebulaColor, indigo, smoothstep(0.66, 0.85, broadCloud) * 0.52);
    color += nebulaColor * nebula * (0.30 + filaments * 0.66) * intensity;
    color += vec3(0.76, 0.24, 0.94) * filaments * 0.28 * intensity;
    color *= 1.0 - dustLane * 0.42 * min(intensity, 1.0);

    // Each bank has a deliberately different heading and speed. The near bank
    // crosses the view quickly, the middle bank counter-drifts, and the far bank
    // moves slowly enough to anchor the scene.
    float farCloud = cloudBank(
        rotateY(direction, time * 0.0030),
        1.62,
        vec3(-time * 0.0024, time * 0.0011, time * 0.0016),
        vec3(23.0, -9.0, 4.0),
        morphTime * 0.035
    );
    float middleCloud = cloudBank(
        rotateY(direction, -time * 0.0085),
        2.36,
        vec3(time * 0.0072, -time * 0.0032, time * 0.0018),
        vec3(-12.0, 7.0, 18.0),
        morphTime * 0.065
    );
    float nearCloud = cloudBank(
        rotateY(direction, time * 0.0150),
        3.28,
        vec3(-time * 0.0130, time * 0.0058, -time * 0.0045),
        vec3(5.0, 21.0, -14.0),
        morphTime * 0.100
    );

    compositeCloudLayer(
        color, farCloud, 0.49, 0.69,
        vec3(0.010, 0.003, 0.034),
        vec3(0.095, 0.018, 0.17),
        vec3(0.36, 0.09, 0.52),
        0.44, intensity
    );
    compositeCloudLayer(
        color, middleCloud, 0.51, 0.70,
        vec3(0.008, 0.004, 0.030),
        vec3(0.065, 0.025, 0.18),
        vec3(0.28, 0.20, 0.58),
        0.60, intensity
    );
    compositeCloudLayer(
        color, nearCloud, 0.53, 0.71,
        vec3(0.006, 0.0015, 0.018),
        vec3(0.048, 0.008, 0.09),
        vec3(0.38, 0.065, 0.42),
        0.74, intensity
    );

    float farDensity = cloudMask(farCloud, 0.49, 0.69);
    float middleDensity = cloudMask(middleCloud, 0.51, 0.70);
    float nearDensity = cloudMask(nearCloud, 0.53, 0.71);
    float combinedCloudDensity = 1.0
            - (1.0 - farDensity * 0.44)
            * (1.0 - middleDensity * 0.60)
            * (1.0 - nearDensity * 0.74);
    float illuminatedEdges = cloudRim(farCloud, 0.49, 0.69) * 0.30
            + cloudRim(middleCloud, 0.51, 0.70) * 0.68
            + cloudRim(nearCloud, 0.53, 0.71);

    // Large morphing voids cut through the luminous material. Their soft outer
    // masks deepen the cloud banks while the cores approach true End-black.
    vec3 voidMorphOffset = vec3(
        sin(morphTime * 0.055 + 1.7),
        cos(morphTime * 0.046 - 0.8),
        sin(morphTime * 0.039 + 2.9)
    ) * 0.90;
    float voidWarpA = valueNoise(direction * 1.08 + voidMorphOffset + vec3(7.0, -3.0, 11.0));
    float voidWarpB = valueNoise(direction.yzx * 1.24 - voidMorphOffset.zxy + vec3(-5.0, 13.0, 2.0));
    vec3 voidWarp = vec3(
        voidWarpA - 0.5,
        voidWarpB - 0.5,
        mix(voidWarpB, voidWarpA, 0.42) - 0.5
    );
    vec3 voidDirection = normalize(
        rotateY(direction, -time * 0.0048) + voidWarp * 0.22
    );
    float voidBroad = fbm(
        voidDirection * 1.72
        + vec3(time * 0.0014, -time * 0.0008, time * 0.0011)
        + vec3(19.0, -6.0, 27.0)
    );
    float voidDetail = valueNoise(
        (voidDirection + voidWarp.zxy * 0.12) * 4.25
        - vec3(time * 0.0031, time * 0.0017, -time * 0.0022)
        + vec3(-8.0, 17.0, 5.0)
    );
    float voidField = voidBroad + (voidDetail - 0.5) * 0.32;
    float configuredVoidCoverage = clamp(CirrusEndVoidCoverage, 0.0, 2.0);
    float voidThresholdShift = (configuredVoidCoverage - 1.0) * 0.16;
    float voidEnabled = smoothstep(0.0, 0.08, configuredVoidCoverage);
    float voidMask = smoothstep(
        0.44 - voidThresholdShift,
        0.66 - voidThresholdShift,
        voidField
    ) * voidEnabled;
    float voidCore = smoothstep(
        0.56 - voidThresholdShift,
        0.73 - voidThresholdShift,
        voidField
    ) * voidEnabled;
    float voidBoundary = max(voidMask - voidCore, 0.0);
    float voidStrength = min(intensity, 1.0)
            * clamp(CirrusEndVoidDarkness, 0.0, 1.0);
    color *= 1.0 - voidMask * 0.76 * voidStrength;
    color = mix(color, vec3(0.0003, 0.0001, 0.002), voidCore * 0.94 * voidStrength);
    color += vec3(0.50, 0.055, 0.58) * voidBoundary * 0.38 * intensity;

    // Darkness surges use a new seeded layout for every event. Randomized start,
    // duration, reach, direction, resistance, and uneven breakup prevent a recognizable cycle.
    float surgeFrequency = max(CirrusEndSurgeFrequency, 0.0);
    float surgeClock = CirrusEndTime * max(surgeFrequency, 0.001);
    float surgeWindow = 47.0;
    float surgeEvent = floor(surgeClock / surgeWindow);
    float surgeAge = mod(surgeClock, surgeWindow);
    vec3 surgeRandom = hash33(vec3(surgeEvent, 211.0, 67.0));
    vec3 surgeAxisRandom = hash33(vec3(surgeEvent, 19.0, 233.0));
    float surgeOccurs = step(0.32, surgeRandom.z) * step(0.001, surgeFrequency);
    float surgeStart = mix(2.5, 12.0, surgeRandom.x);
    float surgeDuration = mix(
        12.0,
        25.0,
        hash13(vec3(surgeEvent, 83.0, 149.0))
    );
    float surgeProgress = (surgeAge - surgeStart) / surgeDuration;
    float surgeGrowth = smoothstep(0.02, 0.58, surgeProgress);

    float surgeY = mix(-0.20, 0.76, surgeAxisRandom.y);
    float surgeAzimuth = surgeAxisRandom.x * PI * 2.0;
    float surgeHorizontal = sqrt(max(1.0 - surgeY * surgeY, 0.0));
    vec3 surgeAxis = vec3(
        cos(surgeAzimuth) * surgeHorizontal,
        surgeY,
        sin(surgeAzimuth) * surgeHorizontal
    );

    float minimumReach = mix(-0.42, 0.08, surgeRandom.y);
    float surgeReach = mix(0.98, minimumReach, surgeGrowth);
    float distortedSurgeFacing = dot(direction, surgeAxis)
            + (voidDetail - 0.5) * 0.18
            + (foldedCloud - 0.5) * 0.08;
    float surgeOuter = smoothstep(
        surgeReach - 0.14,
        surgeReach + 0.06,
        distortedSurgeFacing
    );
    float surgeInner = smoothstep(
        surgeReach + 0.06,
        surgeReach + 0.23,
        distortedSurgeFacing
    );
    float surgeDissolvePattern = clamp(
        voidDetail * 0.55 + foldedCloud * 0.30 + nearCloud * 0.15,
        0.0,
        1.0
    );
    float surgeDissolveThreshold = mix(
        1.12,
        -0.12,
        smoothstep(0.68, 1.0, surgeProgress)
    );
    float surgeDissolve = smoothstep(
        surgeDissolveThreshold - 0.10,
        surgeDissolveThreshold + 0.10,
        surgeDissolvePattern
    );
    float surgeLife = smoothstep(0.0, 0.12, surgeProgress)
            * (1.0 - surgeDissolve);
    float surgeArea = surgeOuter * surgeLife * surgeOccurs;
    float surgeEdge = max(surgeOuter - surgeInner, 0.0)
            * surgeLife
            * surgeOccurs;
    float atmosphereResistance = clamp(
        filaments * 0.72 + illuminatedEdges * 0.34,
        0.0,
        0.75
    );
    float surgeAmount = surgeArea
            * (1.0 - atmosphereResistance)
            * clamp(CirrusEndSurgeStrength, 0.0, 1.0);
    color *= 1.0 - surgeAmount * 0.72;
    color = mix(
        color,
        vec3(0.0002, 0.00005, 0.0015),
        surgeAmount * surgeAmount * 0.68
    );
    color += vec3(0.32, 0.035, 0.44)
            * surgeEdge
            * clamp(CirrusEndSurgeStrength, 0.0, 1.0)
            * 0.18
            * intensity;

    // Major strikes are narrow, forked, and partly hidden inside the clouds.
    float lightningFrequency = max(CirrusEndLightningFrequency, 0.0);
    float lightningClock = CirrusEndTime * max(lightningFrequency, 0.001);
    float lightningIntensity = clamp(CirrusEndLightningIntensity, 0.0, 2.0);
    float lightningWindow = 16.0;
    float lightningEvent = floor(lightningClock / lightningWindow);
    float lightningAge = mod(lightningClock, lightningWindow);
    vec3 lightningRandom = hash33(vec3(lightningEvent, 37.0, 91.0));
    float lightningOccurs = step(0.45, lightningRandom.z)
            * step(0.001, lightningFrequency);
    float firstFlash = 1.0 - smoothstep(0.0, 0.075, lightningAge);
    float secondFlash = smoothstep(0.11, 0.14, lightningAge)
            * (1.0 - smoothstep(0.14, 0.30, lightningAge));
    float lightningFlash = lightningOccurs * max(firstFlash, secondFlash * 0.70);
    float chainDecay = lightningOccurs
            * (1.0 - smoothstep(0.04, 1.55, lightningAge));
    float lightningY = mix(-0.10, 0.70, lightningRandom.y);
    float lightningAzimuth = lightningRandom.x * PI * 2.0;
    float lightningHorizontal = sqrt(max(1.0 - lightningY * lightningY, 0.0));
    vec3 lightningAxis = vec3(
        cos(lightningAzimuth) * lightningHorizontal,
        lightningY,
        sin(lightningAzimuth) * lightningHorizontal
    );

    float lightningAlignment = dot(direction, lightningAxis);
    float lightningHalo = max(lightningFlash, chainDecay * 0.34)
            * smoothstep(0.16, 0.92, lightningAlignment);
    float regionalFlash = max(lightningFlash, chainDecay * 0.18)
            * smoothstep(-0.25, 0.90, lightningAlignment);
    color += vec3(0.30, 0.18, 0.60)
            * regionalFlash
            * (0.22 + combinedCloudDensity * 0.78)
            * intensity
            * lightningIntensity;
    float layeredIllumination = clamp(
        farDensity * 0.26 + middleDensity * 0.66 + nearDensity,
        0.0,
        1.45
    );
    color += vec3(0.48, 0.30, 0.86)
            * lightningHalo
            * (layeredIllumination * 0.82 + illuminatedEdges * 0.80 + voidBoundary * 0.52 + surgeEdge * 0.38)
            * 1.34
            * intensity
            * lightningIntensity;
    color += vec3(0.56, 0.66, 1.00)
            * lightningFlash
            * smoothstep(0.62, 0.97, lightningAlignment)
            * illuminatedEdges
            * 0.62
            * intensity
            * lightningIntensity;

    float chain = chainLightning(direction, lightningAxis, lightningRandom.x);
    float dissolveNoise = hash13(floor(direction * 420.0) + lightningEvent * 13.7);
    float chainDissolve = smoothstep(0.0, 0.34, chainDecay - dissolveNoise * 0.52);
    float chainVisibility = mix(
        0.04,
        0.74,
        smoothstep(0.10, 0.68, combinedCloudDensity)
    ) * (0.78 + illuminatedEdges * 0.22);
    color += vec3(0.72, 0.78, 1.00)
            * chain
            * max(lightningFlash, chainDecay * chainDissolve)
            * chainVisibility
            * 1.28
            * intensity
            * lightningIntensity;

    // A separate, faster event clock creates small flashes behind the far and
    // middle banks. These are dimmer, shorter, and only expose distant depth.
    float distantWindow = 11.0;
    float distantClock = lightningClock + 4.0;
    float distantEvent = floor(distantClock / distantWindow);
    float distantAge = mod(distantClock, distantWindow);
    vec3 distantRandom = hash33(vec3(distantEvent, 173.0, 29.0));
    float distantOccurs = step(0.30, distantRandom.z)
            * step(0.001, lightningFrequency);
    float distantFirst = 1.0 - smoothstep(0.0, 0.08, distantAge);
    float distantSecond = smoothstep(0.18, 0.21, distantAge)
            * (1.0 - smoothstep(0.21, 0.38, distantAge));
    float distantFlash = distantOccurs * max(distantFirst, distantSecond * 0.48);
    float distantY = mix(0.02, 0.76, distantRandom.y);
    float distantAzimuth = distantRandom.x * PI * 2.0 + 1.7;
    float distantHorizontal = sqrt(max(1.0 - distantY * distantY, 0.0));
    vec3 distantAxis = vec3(
        cos(distantAzimuth) * distantHorizontal,
        distantY,
        sin(distantAzimuth) * distantHorizontal
    );
    float distantFacing = smoothstep(0.38, 0.95, dot(direction, distantAxis));
    float distantClouds = clamp(farDensity * 0.82 + middleDensity * 0.26, 0.0, 1.0);
    color += vec3(0.16, 0.10, 0.38)
            * distantFlash
            * distantFacing
            * (0.18 + farDensity * 0.42)
            * intensity
            * lightningIntensity;
    color += vec3(0.30, 0.22, 0.68)
            * distantFlash
            * distantFacing
            * distantClouds
            * 0.88
            * intensity
            * lightningIntensity;
    float distantBolt = lightningBolt(direction, distantAxis, distantRandom.x);
    color += vec3(0.48, 0.56, 0.92)
            * distantBolt
            * distantFlash
            * farDensity
            * 0.26
            * intensity
            * lightningIntensity;

    float faintStars = starLayer(direction, 118.0, 0.972);
    float brightStars = starLayer(direction, 61.0, 0.982);
    float starColorNoise = hash13(floor(direction * 61.0) + 8.0);
    vec3 starColor = mix(vec3(0.58, 0.72, 1.0), vec3(1.0, 0.58, 0.92), starColorNoise);
    float starVisibility = (1.0 - combinedCloudDensity * (0.86 - voidMask * 0.55))
            * (1.0 - surgeAmount * 0.55);
    color += vec3(0.64, 0.58, 0.90) * faintStars * 0.52 * intensity * starVisibility;
    color += starColor * brightStars * 1.34 * intensity * starVisibility;

    float shards = shardLayer(direction);
    color += vec3(0.45, 0.08, 0.72) * shards * intensity;

    float softPulse = 0.975 + 0.025 * sin(time * 0.07);
    color *= softPulse;
    color = color / (color + vec3(0.78));
    color = pow(max(color, vec3(0.0)), vec3(0.82));

    // Collapse low and middle values toward End-black without muting the rare
    // luminous filaments, stars, or lightning flashes.
    float finalLuminance = max(color.r, max(color.g, color.b));
    float highlightPreservation = smoothstep(0.16, 0.64, finalLuminance);
    color *= mix(0.30, 1.0, highlightPreservation);
    fragColor = vec4(color, 1.0);
}
