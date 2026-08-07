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

void main() {
    vec3 smoothDirection = normalize(worldDirection);
    vec3 direction = mix(smoothDirection, pixelatedDirection(smoothDirection), clamp(CirrusEndPixelation, 0.0, 1.0));
    float intensity = clamp(CirrusEndIntensity, 0.0, 2.0);
    float time = CirrusEndTime * max(CirrusEndAnimationSpeed, 0.0);
    vec3 driftingDirection = rotateY(direction, time * 0.0022);

    float vertical = direction.y * 0.5 + 0.5;
    vec3 color = mix(vec3(0.006, 0.002, 0.018), vec3(0.022, 0.006, 0.050), vertical);
    color += vec3(0.018, 0.004, 0.030) * (1.0 - abs(direction.y));

    vec3 drift = vec3(time * 0.0040, -time * 0.0026, time * 0.0017);
    float broadCloud = fbm(driftingDirection * 2.55 + drift);
    float foldedCloud = fbm(driftingDirection * 5.7 - drift * 1.7 + vec3(11.0, 4.0, -8.0));
    float fineCloud = fbm(driftingDirection * 11.8 + drift * 2.4 + vec3(-6.0, 15.0, 3.0));
    float nebula = smoothstep(0.46, 0.78, broadCloud + foldedCloud * 0.20);
    float filaments = pow(clamp(1.0 - abs(foldedCloud * 2.0 - 1.0), 0.0, 1.0), 4.0) * nebula;
    float dustLane = smoothstep(0.52, 0.73, fineCloud) * smoothstep(0.28, 0.74, broadCloud);

    vec3 violet = vec3(0.31, 0.055, 0.56);
    vec3 magenta = vec3(0.72, 0.075, 0.48);
    vec3 cyan = vec3(0.055, 0.46, 0.58);
    vec3 nebulaColor = mix(violet, magenta, smoothstep(0.38, 0.72, foldedCloud));
    nebulaColor = mix(nebulaColor, cyan, smoothstep(0.67, 0.85, broadCloud) * 0.58);
    color += nebulaColor * nebula * (0.42 + filaments * 0.72) * intensity;
    color += vec3(0.82, 0.28, 0.92) * filaments * 0.22 * intensity;
    color *= 1.0 - dustLane * 0.43 * min(intensity, 1.0);

    // A separate, slower noise field forms broad banks with dense interiors
    // and bright scalloped edges, keeping them distinct from the nebulas.
    vec3 cloudDrift = vec3(-time * 0.0014, time * 0.0007, time * 0.0011);
    float cloudCoverage = fbm(rotateY(direction, -time * 0.0008) * 2.05 + cloudDrift + vec3(23.0, -9.0, 4.0));
    float cloudBillows = fbm(direction * 5.4 - cloudDrift * 1.8 + vec3(-12.0, 7.0, 18.0));
    float cloudDetail = fbm(direction * 12.5 + cloudDrift * 3.1 + vec3(5.0, 21.0, -14.0));
    float cloudField = cloudCoverage + (cloudBillows - 0.5) * 0.32 + (cloudDetail - 0.5) * 0.10;
    float cloudDensity = smoothstep(0.51, 0.69, cloudField);
    float cloudInterior = smoothstep(0.63, 0.79, cloudField);
    float cloudEdge = clamp(cloudDensity - cloudInterior, 0.0, 1.0);

    color *= 1.0 - cloudDensity * 0.34 * min(intensity, 1.0);
    color += vec3(0.075, 0.018, 0.135) * cloudDensity * intensity;
    color += mix(vec3(0.20, 0.07, 0.34), vec3(0.08, 0.30, 0.39), cloudCoverage)
            * cloudEdge * 0.72 * intensity;

    // Each 23-second window has a modest chance to produce a short double
    // flash, placing the bolt at a new deterministic point in the sky.
    float lightningWindow = 23.0;
    float lightningEvent = floor(CirrusEndTime / lightningWindow);
    float lightningAge = mod(CirrusEndTime, lightningWindow);
    vec3 lightningRandom = hash33(vec3(lightningEvent, 37.0, 91.0));
    float lightningOccurs = step(0.58, lightningRandom.z);
    float firstFlash = 1.0 - smoothstep(0.0, 0.11, lightningAge);
    float secondFlash = smoothstep(0.15, 0.18, lightningAge) * (1.0 - smoothstep(0.18, 0.34, lightningAge));
    float lightningFlash = lightningOccurs * max(firstFlash, secondFlash * 0.56);
    float lightningY = mix(-0.12, 0.68, lightningRandom.y);
    float lightningAzimuth = lightningRandom.x * PI * 2.0;
    float lightningHorizontal = sqrt(max(1.0 - lightningY * lightningY, 0.0));
    vec3 lightningAxis = vec3(
        cos(lightningAzimuth) * lightningHorizontal,
        lightningY,
        sin(lightningAzimuth) * lightningHorizontal
    );
    float lightningFacing = smoothstep(0.45, 0.93, dot(direction, lightningAxis));
    float bolt = lightningBolt(direction, lightningAxis, lightningRandom.x);
    float cloudIllumination = lightningFlash * lightningFacing * cloudDensity;
    color += vec3(0.32, 0.24, 0.58) * cloudIllumination * 1.25 * intensity;
    color += vec3(0.72, 0.82, 1.00) * bolt * lightningFlash * 2.4 * intensity;

    float faintStars = starLayer(direction, 118.0, 0.972);
    float brightStars = starLayer(direction, 61.0, 0.982);
    float starColorNoise = hash13(floor(direction * 61.0) + 8.0);
    vec3 starColor = mix(vec3(0.58, 0.72, 1.0), vec3(1.0, 0.58, 0.92), starColorNoise);
    float starVisibility = 1.0 - cloudDensity * 0.78;
    color += vec3(0.64, 0.58, 0.90) * faintStars * 0.52 * intensity * starVisibility;
    color += starColor * brightStars * 1.34 * intensity * starVisibility;

    float shards = shardLayer(direction);
    color += vec3(0.45, 0.08, 0.72) * shards * intensity;

    float softPulse = 0.975 + 0.025 * sin(time * 0.07);
    color *= softPulse;
    color = color / (color + vec3(0.78));
    color = pow(max(color, vec3(0.0)), vec3(0.82));
    fragColor = vec4(color, 1.0);
}
