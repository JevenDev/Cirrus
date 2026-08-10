#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float CirrusEnabled;
uniform vec2 CirrusLightDirection;
uniform vec3 CirrusLightColor;
uniform float CirrusSunWeight;
uniform vec3 CirrusLightViewDirection;
uniform float CirrusRainLevel;
uniform float CirrusThunderLevel;
uniform float CirrusLightningFlash;
uniform vec3 CirrusLightningViewPosition;
uniform vec3 CirrusWorldUpViewDirection;
uniform float CirrusLightningRadius;

in vec2 texCoord0;
in float vertexDistance;
in vec4 vertexColor;
in vec3 viewDirection;
in vec3 viewNormal;

out vec4 fragColor;

const float COS_5_DEGREES = 0.9961947;
const float COS_7_DEGREES = 0.9925462;
const float COS_30_DEGREES = 0.8660254;
const float COS_38_DEGREES = 0.7880108;

void main() {
    float rainLevel = clamp(CirrusRainLevel, 0.0, 1.0);
    float thunderLevel = clamp(CirrusThunderLevel, 0.0, 1.0);
    vec4 cloudSample = texture(Sampler0, texCoord0);
    vec4 color = cloudSample * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }

    if (CirrusEnabled > 0.5) {
        vec2 textureSizePixels = vec2(textureSize(Sampler0, 0));
        vec2 positionInTexel = fract(texCoord0 * textureSizePixels);

        float emptyLeft = 1.0 - step(0.1, textureOffset(Sampler0, texCoord0, ivec2(-1, 0)).a);
        float emptyRight = 1.0 - step(0.1, textureOffset(Sampler0, texCoord0, ivec2(1, 0)).a);
        float emptyBottom = 1.0 - step(0.1, textureOffset(Sampler0, texCoord0, ivec2(0, -1)).a);
        float emptyTop = 1.0 - step(0.1, textureOffset(Sampler0, texCoord0, ivec2(0, 1)).a);

        float lightStrength = length(CirrusLightDirection);
        float horizonAmount = smoothstep(0.05, 1.0, lightStrength);
        float fadeWidth = mix(0.32, 0.62, horizonAmount);
        float fadeLeft = emptyLeft * (1.0 - smoothstep(0.0, fadeWidth, positionInTexel.x));
        float fadeRight = emptyRight * (1.0 - smoothstep(0.0, fadeWidth, 1.0 - positionInTexel.x));
        float fadeBottom = emptyBottom * (1.0 - smoothstep(0.0, fadeWidth, positionInTexel.y));
        float fadeTop = emptyTop * (1.0 - smoothstep(0.0, fadeWidth, 1.0 - positionInTexel.y));

        vec2 lightAxis = lightStrength > 0.001 ? normalize(CirrusLightDirection) : vec2(0.0);
        float sunEdgeFade = max(
            max(
                fadeLeft * max(0.0, dot(vec2(-1.0, 0.0), lightAxis)),
                fadeRight * max(0.0, dot(vec2(1.0, 0.0), lightAxis))
            ),
            max(
                fadeBottom * max(0.0, dot(vec2(0.0, -1.0), lightAxis)),
                fadeTop * max(0.0, dot(vec2(0.0, 1.0), lightAxis))
            )
        );

        vec3 normalizedViewDirection = normalize(viewDirection);
        vec3 sunViewDirection = normalize(CirrusLightViewDirection);
        float sunAlignment = dot(normalizedViewDirection, sunViewDirection);
        float sunProximity = smoothstep(COS_38_DEGREES, COS_5_DEGREES, sunAlignment);

        float sunOcclusion = smoothstep(COS_30_DEGREES, COS_7_DEGREES, sunAlignment);
        color.a = mix(color.a, 1.0, sunOcclusion * CirrusSunWeight);

        // Keep the pixel silhouette untouched. Only the lighting fades inward from
        // square exposed edges, with a restrained lift instead of an opaque halo.
        float sunHighlight = sunEdgeFade * horizonAmount * CirrusSunWeight * sunProximity * 0.20;
        vec3 sunEdgeColor = mix(color.rgb, CirrusLightColor, 0.35);
        color.rgb = mix(color.rgb, sunEdgeColor, sunHighlight);

        // Recolor the whole cloudscape as the sun crosses the horizon
        vec3 worldUpViewDirection = normalize(CirrusWorldUpViewDirection);
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

        float moonAlignment = dot(normalizedViewDirection, -sunViewDirection);
        float moonward = smoothstep(0.05, 0.96, moonAlignment);
        float nightAmount = (1.0 - CirrusSunWeight) * (1.0 - twilightAmount * 0.72);
        float nightStrength = nightAmount * clearSkyAmount * mix(0.20, 0.36, moonward);
        color.rgb = mix(color.rgb, color.rgb * vec3(0.72, 0.84, 1.12), nightStrength);
    }

    color.rgb *= mix(1.0, 0.92, rainLevel);
    color.rgb *= mix(1.0, 0.84, thunderLevel);
    float lightningFlash = clamp(CirrusLightningFlash, 0.0, 1.0);
    if (lightningFlash > 0.001) {
    vec3 lightningOffset = viewDirection - CirrusLightningViewPosition;
    vec3 viewUp = normalize(CirrusWorldUpViewDirection);
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
