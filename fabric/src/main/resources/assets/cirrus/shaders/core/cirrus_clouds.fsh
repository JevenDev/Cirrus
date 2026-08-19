#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    vec4 ColorModulator;
    float FogStart;
    float FogEnd;
    vec4 FogColor;
    float FogShape;
    float CirrusEnabled;
    vec2 CirrusLightDirection;
    float CirrusSunWeight;
    vec4 CirrusLightViewDirection;
    float CirrusRainLevel;
    float CirrusThunderLevel;
    float CirrusRainCloudCoverage;
    float CirrusThunderCloudCoverage;
    float CirrusLightningFlash;
    vec4 CirrusLightningViewPosition;
    vec4 CirrusWorldUpViewDirection;
    float CirrusLightningRadius;
};


uniform sampler2D Sampler0;


in vec2 texCoord0;
in float vertexDistance;
in vec4 vertexColor;
in vec3 viewDirection;

out vec4 fragColor;

vec4 linear_fog(vec4 color, float distanceValue, float fogStart, float fogEnd, vec4 fogColor) {
    float amount = clamp((distanceValue - fogStart) / max(fogEnd - fogStart, 0.0001), 0.0, 1.0);
    return vec4(mix(color.rgb, fogColor.rgb, amount * fogColor.a), color.a);
}

const float COS_7_DEGREES = 0.9925462;
const float COS_30_DEGREES = 0.8660254;

const vec2 RAIN_CLOUD_PATTERN_OFFSET = vec2(83.0, 47.0);
const vec2 THUNDER_CLOUD_PATTERN_OFFSET = vec2(157.0, 109.0);

void main() {
    float rainLevel = clamp(CirrusRainLevel, 0.0, 1.0);
    float thunderLevel = clamp(CirrusThunderLevel, 0.0, 1.0);
    float rainCoverage = clamp(CirrusRainCloudCoverage, 0.0, 1.0);
    float thunderCoverage = clamp(CirrusThunderCloudCoverage, 0.0, 1.0);
    vec2 textureSizePixels = vec2(textureSize(Sampler0, 0));
    vec2 texelSize = 1.0 / textureSizePixels;
    vec4 cloudSample = texture(Sampler0, texCoord0);
    vec4 rainCloudSample = vec4(0.0);
    vec4 thunderCloudSample = vec4(0.0);
    if (rainCoverage > 0.001) {
        rainCloudSample = texture(Sampler0, texCoord0 + RAIN_CLOUD_PATTERN_OFFSET * texelSize);
    }
    if (thunderCoverage > 0.001) {
        thunderCloudSample = texture(Sampler0, texCoord0 + THUNDER_CLOUD_PATTERN_OFFSET * texelSize);
    }
    float baseCloudAlpha = cloudSample.a;
    float rainCloudAlpha = rainCloudSample.a * rainCoverage;
    float thunderCloudAlpha = thunderCloudSample.a * thunderCoverage;
    float supplementalCloudAlpha =
        1.0 - (1.0 - rainCloudAlpha) * (1.0 - thunderCloudAlpha);
    vec4 dominantWeatherSample =
        rainCloudAlpha >= thunderCloudAlpha ? rainCloudSample : thunderCloudSample;
    if (supplementalCloudAlpha > baseCloudAlpha) {
        cloudSample.rgb = dominantWeatherSample.rgb;
    }
    cloudSample.a =
        1.0 - (1.0 - baseCloudAlpha) * (1.0 - supplementalCloudAlpha);
    vec4 color = cloudSample * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }

    if (CirrusEnabled > 0.5) {
        float lightStrength = length(CirrusLightDirection);
        float horizonAmount = smoothstep(0.05, 1.0, lightStrength);

        vec3 normalizedViewDirection = normalize(viewDirection);
        vec3 sunViewDirection = normalize(CirrusLightViewDirection.xyz);
        float sunAlignment = dot(normalizedViewDirection, sunViewDirection);
        float moonAlignment = dot(normalizedViewDirection, -sunViewDirection);
        vec3 worldUpViewDirection = normalize(CirrusWorldUpViewDirection.xyz);

        float sunIllumination = smoothstep(COS_30_DEGREES, COS_7_DEGREES, sunAlignment);
        color.a = mix(color.a, 1.0, sunIllumination * CirrusSunWeight);

        // Recolor the whole cloudscape as the sun crosses the horizon
        float viewHeight = abs(dot(normalizedViewDirection, worldUpViewDirection));
        float horizonBand = 1.0 - smoothstep(0.12, 0.72, viewHeight);
        float twilightAmount = smoothstep(0.82, 0.995, horizonAmount);
        float clearSkyAmount = 1.0 - clamp(rainLevel * 0.65 + thunderLevel * 0.55, 0.0, 0.88);

        float sunward = smoothstep(-0.30, 0.92, sunAlignment);
        float sunCore = smoothstep(0.55, 0.992, sunAlignment);
        vec3 farTwilightTint = vec3(0.84, 0.70, 1.04);
        vec3 nearTwilightTint = vec3(1.26, 0.70, 0.42);
        vec3 twilightTint = mix(farTwilightTint, nearTwilightTint, sunward);
        twilightTint = mix(twilightTint, vec3(1.34, 0.82, 0.48), sunCore);
        float twilightStrength = twilightAmount
            * clearSkyAmount
            * mix(0.24, 0.82, sunward)
            * mix(0.72, 1.0, horizonBand);
        color.rgb = mix(color.rgb, color.rgb * twilightTint, twilightStrength);

        // Noon keeps a restrained warm-facing side, while moonlit clouds cool
        // toward blue. Both effects use sky position instead of a flat global tint.
        float daylightAmount = CirrusSunWeight * (1.0 - twilightAmount);
        float daySunward = smoothstep(0.10, 0.96, sunAlignment);
        float daylightStrength = daylightAmount * daySunward * clearSkyAmount * 0.18;
        color.rgb = mix(color.rgb, color.rgb * vec3(1.05, 1.015, 0.94), daylightStrength);

        float moonward = smoothstep(0.05, 0.96, moonAlignment);
        float nightAmount = (1.0 - CirrusSunWeight) * (1.0 - twilightAmount * 0.72);
        float nightStrength = nightAmount * clearSkyAmount * mix(0.20, 0.36, moonward);
        color.rgb = mix(color.rgb, color.rgb * vec3(0.72, 0.84, 1.12), nightStrength);
    }

    color.rgb *= mix(1.0, 0.92, rainLevel);
    color.rgb *= mix(1.0, 0.84, thunderLevel);
    float lightningFlash = clamp(CirrusLightningFlash, 0.0, 1.0);
    if (lightningFlash > 0.001) {
    vec3 lightningOffset = viewDirection - CirrusLightningViewPosition.xyz;
    vec3 viewUp = normalize(CirrusWorldUpViewDirection.xyz);
    vec3 horizontalOffset = lightningOffset - viewUp * dot(lightningOffset, viewUp);
    float lightningRadius = max(CirrusLightningRadius, 1.0);
    float lightningDistance = length(horizontalOffset);
    float normalizedLightningDistance = lightningDistance / lightningRadius;
    float regionalFlash = 1.0 - smoothstep(0.08, 0.95, normalizedLightningDistance);
    float localFlash = 1.0 - smoothstep(0.015, 0.42, normalizedLightningDistance);
    float channelFlash = 1.0 - smoothstep(0.0, 0.13, normalizedLightningDistance);

    float neighboringAlpha = min(
        min(
            textureOffset(Sampler0, texCoord0, ivec2(-1, 0)).a,
            textureOffset(Sampler0, texCoord0, ivec2(1, 0)).a
        ),
        min(
            textureOffset(Sampler0, texCoord0, ivec2(0, -1)).a,
            textureOffset(Sampler0, texCoord0, ivec2(0, 1)).a
        )
    );
    float exposedEdge = 1.0 - smoothstep(0.08, 0.55, neighboringAlpha);
    float nearbyDensity = 0.125 * (
        textureOffset(Sampler0, texCoord0, ivec2(-1, 0)).a
        + textureOffset(Sampler0, texCoord0, ivec2(1, 0)).a
        + textureOffset(Sampler0, texCoord0, ivec2(0, -1)).a
        + textureOffset(Sampler0, texCoord0, ivec2(0, 1)).a
        + textureOffset(Sampler0, texCoord0, ivec2(-2, -2)).a
        + textureOffset(Sampler0, texCoord0, ivec2(2, -2)).a
        + textureOffset(Sampler0, texCoord0, ivec2(-2, 2)).a
        + textureOffset(Sampler0, texCoord0, ivec2(2, 2)).a
    );
    float wideDensity = 0.25 * (
        textureOffset(Sampler0, texCoord0, ivec2(-4, 0)).a
        + textureOffset(Sampler0, texCoord0, ivec2(4, 0)).a
        + textureOffset(Sampler0, texCoord0, ivec2(0, -4)).a
        + textureOffset(Sampler0, texCoord0, ivec2(0, 4)).a
    );
    float densityCavity = 1.0 - smoothstep(0.22, 0.90, mix(nearbyDensity, wideDensity, 0.42));
    float densityContrast = clamp(abs(cloudSample.a - nearbyDensity) * 2.8, 0.0, 1.0);
    float illuminatedFold = clamp(
        exposedEdge * 0.50 + densityCavity * 0.34 + densityContrast * 0.56,
        0.0,
        1.0
    );

    vec3 normalizedCloudDirection = normalize(viewDirection);
    float cloudAboveViewer = smoothstep(-0.04, 0.46, dot(normalizedCloudDirection, viewUp));
    float surfaceResponse = mix(0.78, 1.16, cloudAboveViewer);
    float flashEnergy = lightningFlash * surfaceResponse;

    vec3 coolScatter = vec3(0.42, 0.50, 0.82);
    vec3 hotScatter = vec3(0.96, 0.97, 1.0);
    float whiteHeat = clamp(localFlash * 0.68 + illuminatedFold * 0.46, 0.0, 1.0);
    vec3 lightningColor = mix(coolScatter, hotScatter, whiteHeat);
    color.rgb *= mix(vec3(1.0), vec3(0.76, 0.81, 1.06), regionalFlash * flashEnergy * 0.26);
    color.rgb += lightningColor * flashEnergy * (
        regionalFlash * (0.10 + illuminatedFold * 0.12)
        + localFlash * (0.34 + illuminatedFold * 0.42)
        + channelFlash * (0.22 + illuminatedFold * 0.15)
        + exposedEdge * localFlash * 0.18
    );
    color.a = mix(
        color.a,
        1.0,
        localFlash * lightningFlash * (0.10 + illuminatedFold * 0.18)
    );
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
