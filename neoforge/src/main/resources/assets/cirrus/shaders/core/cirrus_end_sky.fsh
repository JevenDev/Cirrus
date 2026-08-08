#version 150

uniform float CirrusEndTime;
uniform float CirrusEndIntensity;
uniform float CirrusEndAnimationSpeed;
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

float lightningBolt(vec3 direction, vec3 axis, float seed) {
    vec3 tangent = normalize(cross(axis, abs(axis.y) > 0.8 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0)));
    vec3 bitangent = cross(axis, tangent);
    float facing = dot(direction, axis);
    vec2 boltUv = vec2(dot(direction, tangent), dot(direction, bitangent)) / max(facing, 0.2);

    float crookedCenter = sin(boltUv.y * 24.0 + seed * 19.0) * 0.018;
    crookedCenter += sin(boltUv.y * 57.0 - seed * 31.0) * 0.009;
    crookedCenter += sin(boltUv.y * 113.0 + seed * 7.0) * 0.004;
    float verticalMask = smoothstep(-0.38, -0.30, boltUv.y) * (1.0 - smoothstep(0.28, 0.38, boltUv.y));
    float mainDistance = abs(boltUv.x - crookedCenter);
    float mainBolt = (1.0 - smoothstep(0.004, 0.014, mainDistance)) * verticalMask;

    float branchWindow = smoothstep(-0.04, 0.02, boltUv.y) * (1.0 - smoothstep(0.15, 0.23, boltUv.y));
    float branchCenter = crookedCenter + (boltUv.y - 0.08) * 0.72;
    branchCenter += sin(boltUv.y * 76.0 + seed * 43.0) * 0.010;
    float branch = (1.0 - smoothstep(0.004, 0.013, abs(boltUv.x - branchCenter))) * branchWindow;
    return (mainBolt + branch * 0.72) * smoothstep(0.68, 0.86, facing);
}

float lightningSegment(vec2 uv, vec2 startPoint, vec2 endPoint, float seed, float width) {
    vec2 segment = endPoint - startPoint;
    float progress = clamp(dot(uv - startPoint, segment) / dot(segment, segment), 0.0, 1.0);
    vec2 normal = normalize(vec2(-segment.y, segment.x));
    float crooked = sin(progress * 31.0 + seed * 17.0) * width * 1.8;
    crooked += sin(progress * 83.0 - seed * 29.0) * width * 0.72;
    vec2 closest = mix(startPoint, endPoint, progress) + normal * crooked;
    float distanceToSegment = length(uv - closest);
    float core = 1.0 - smoothstep(width * 0.28, width, distanceToSegment);
    float glow = 1.0 - smoothstep(width, width * 4.5, distanceToSegment);
    return core + glow * 0.22;
}

float chainLightning(vec3 direction, vec3 axis, float seed) {
    vec3 tangent = normalize(cross(axis, abs(axis.y) > 0.8 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0)));
    vec3 bitangent = cross(axis, tangent);
    float facing = dot(direction, axis);
    vec2 uv = vec2(dot(direction, tangent), dot(direction, bitangent)) / max(facing, 0.24);

    float bendA = (seed - 0.5) * 0.08;
    float bendB = (fract(seed * 7.31) - 0.5) * 0.10;
    vec2 point0 = vec2(-0.34, 0.12 + bendA);
    vec2 point1 = vec2(-0.18, -0.015 + bendB);
    vec2 point2 = vec2(-0.015, 0.075 - bendA);
    vec2 point3 = vec2(0.16, -0.045 - bendB);
    vec2 point4 = vec2(0.34, 0.035 + bendA);

    float chain = lightningSegment(uv, point0, point1, seed + 1.0, 0.0070);
    chain += lightningSegment(uv, point1, point2, seed + 2.0, 0.0062);
    chain += lightningSegment(uv, point2, point3, seed + 3.0, 0.0055);
    chain += lightningSegment(uv, point3, point4, seed + 4.0, 0.0048);
    chain += lightningSegment(uv, point1, vec2(-0.10, -0.19), seed + 5.0, 0.0044) * 0.72;
    chain += lightningSegment(uv, point2, vec2(0.08, 0.24), seed + 6.0, 0.0040) * 0.64;
    chain += lightningSegment(uv, point3, vec2(0.25, -0.16), seed + 7.0, 0.0036) * 0.52;
    return chain * smoothstep(0.58, 0.87, facing);
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
        time * 0.035
    );
    float middleCloud = cloudBank(
        rotateY(direction, -time * 0.0085),
        2.36,
        vec3(time * 0.0072, -time * 0.0032, time * 0.0018),
        vec3(-12.0, 7.0, 18.0),
        time * 0.065
    );
    float nearCloud = cloudBank(
        rotateY(direction, time * 0.0150),
        3.28,
        vec3(-time * 0.0130, time * 0.0058, -time * 0.0045),
        vec3(5.0, 21.0, -14.0),
        time * 0.100
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
        sin(time * 0.055 + 1.7),
        cos(time * 0.046 - 0.8),
        sin(time * 0.039 + 2.9)
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
    float voidMask = smoothstep(0.44, 0.66, voidField);
    float voidCore = smoothstep(0.56, 0.73, voidField);
    float voidBoundary = max(voidMask - voidCore, 0.0);
    float voidStrength = min(intensity, 1.0);
    color *= 1.0 - voidMask * 0.76 * voidStrength;
    color = mix(color, vec3(0.0003, 0.0001, 0.002), voidCore * 0.94 * voidStrength);
    color += vec3(0.50, 0.055, 0.58) * voidBoundary * 0.38 * intensity;

    // Major chain strikes are rare and linger as broken, fading afterimages.
    float lightningWindow = 16.0;
    float lightningEvent = floor(CirrusEndTime / lightningWindow);
    float lightningAge = mod(CirrusEndTime, lightningWindow);
    vec3 lightningRandom = hash33(vec3(lightningEvent, 37.0, 91.0));
    float lightningOccurs = step(0.45, lightningRandom.z);
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
            * intensity;
    float layeredIllumination = clamp(
        farDensity * 0.26 + middleDensity * 0.66 + nearDensity,
        0.0,
        1.45
    );
    color += vec3(0.48, 0.30, 0.86)
            * lightningHalo
            * (layeredIllumination * 0.82 + illuminatedEdges * 0.80 + voidBoundary * 0.52)
            * 1.34
            * intensity;
    color += vec3(0.56, 0.66, 1.00)
            * lightningFlash
            * smoothstep(0.62, 0.97, lightningAlignment)
            * illuminatedEdges
            * 0.62
            * intensity;

    float chain = chainLightning(direction, lightningAxis, lightningRandom.x);
    float dissolveNoise = hash13(floor(direction * 420.0) + lightningEvent * 13.7);
    float chainDissolve = smoothstep(0.0, 0.34, chainDecay - dissolveNoise * 0.52);
    float chainVisibility = mix(0.18, 1.0, smoothstep(0.08, 0.66, combinedCloudDensity));
    color += vec3(0.76, 0.82, 1.00)
            * chain
            * max(lightningFlash, chainDecay * chainDissolve)
            * chainVisibility
            * 2.05
            * intensity;

    // A separate, faster event clock creates small flashes behind the far and
    // middle banks. These are dimmer, shorter, and only expose distant depth.
    float distantWindow = 11.0;
    float distantEvent = floor((CirrusEndTime + 4.0) / distantWindow);
    float distantAge = mod(CirrusEndTime + 4.0, distantWindow);
    vec3 distantRandom = hash33(vec3(distantEvent, 173.0, 29.0));
    float distantOccurs = step(0.30, distantRandom.z);
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
            * intensity;
    color += vec3(0.30, 0.22, 0.68)
            * distantFlash
            * distantFacing
            * distantClouds
            * 0.88
            * intensity;
    float distantBolt = lightningBolt(direction, distantAxis, distantRandom.x);
    color += vec3(0.48, 0.56, 0.92)
            * distantBolt
            * distantFlash
            * farDensity
            * 0.42
            * intensity;

    float faintStars = starLayer(direction, 118.0, 0.972);
    float brightStars = starLayer(direction, 61.0, 0.982);
    float starColorNoise = hash13(floor(direction * 61.0) + 8.0);
    vec3 starColor = mix(vec3(0.58, 0.72, 1.0), vec3(1.0, 0.58, 0.92), starColorNoise);
    float starVisibility = 1.0 - combinedCloudDensity * (0.86 - voidMask * 0.55);
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
