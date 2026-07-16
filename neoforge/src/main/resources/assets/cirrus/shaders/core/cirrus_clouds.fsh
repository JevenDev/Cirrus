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
    }

    color.rgb *= mix(1.0, 0.92, rainLevel);
    color.rgb *= mix(1.0, 0.84, thunderLevel);
    float lightningFlash = clamp(CirrusLightningFlash, 0.0, 1.0);
    vec3 lightningOffset = viewDirection - CirrusLightningViewPosition;
    vec3 viewUp = normalize(CirrusWorldUpViewDirection);
    vec3 horizontalOffset = lightningOffset - viewUp * dot(lightningOffset, viewUp);
    float lightningRadius = max(CirrusLightningRadius, 1.0);
    float lightningDistance = length(horizontalOffset);
    float lightningFalloff = 1.0 - smoothstep(lightningRadius * 0.12, lightningRadius, lightningDistance);
    float localizedFlash = clamp(lightningFlash * lightningFalloff, 0.0, 1.0);
    color.rgb = mix(color.rgb, vec3(0.86, 0.90, 1.0), localizedFlash);

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
