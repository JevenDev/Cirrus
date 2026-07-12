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

in vec2 texCoord0;
in float vertexDistance;
in vec4 vertexColor;
in vec3 viewDirection;

out vec4 fragColor;

void main() {
    vec4 cloudSample = texture(Sampler0, texCoord0);
    vec4 color = cloudSample * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }

    if (CirrusEnabled > 0.5) {
        vec2 texelSize = 1.0 / vec2(textureSize(Sampler0, 0));
        vec2 positionInTexel = fract(texCoord0 / texelSize);

        float emptyLeft = 1.0 - step(0.1, texture(Sampler0, texCoord0 - vec2(texelSize.x, 0.0)).a);
        float emptyRight = 1.0 - step(0.1, texture(Sampler0, texCoord0 + vec2(texelSize.x, 0.0)).a);
        float emptyBottom = 1.0 - step(0.1, texture(Sampler0, texCoord0 - vec2(0.0, texelSize.y)).a);
        float emptyTop = 1.0 - step(0.1, texture(Sampler0, texCoord0 + vec2(0.0, texelSize.y)).a);

        float lightStrength = length(CirrusLightDirection);
        float horizonAmount = smoothstep(0.05, 1.0, lightStrength);
        float fadeWidth = mix(0.14, 0.52, horizonAmount);
        float fadeLeft = emptyLeft * (1.0 - smoothstep(0.0, fadeWidth, positionInTexel.x));
        float fadeRight = emptyRight * (1.0 - smoothstep(0.0, fadeWidth, 1.0 - positionInTexel.x));
        float fadeBottom = emptyBottom * (1.0 - smoothstep(0.0, fadeWidth, positionInTexel.y));
        float fadeTop = emptyTop * (1.0 - smoothstep(0.0, fadeWidth, 1.0 - positionInTexel.y));

        vec2 lightAxis = lightStrength > 0.001 ? normalize(CirrusLightDirection) : vec2(0.0);
        float sunDirectionalEdge = max(
            max(
                fadeLeft * max(0.0, dot(vec2(-1.0, 0.0), lightAxis)),
                fadeRight * max(0.0, dot(vec2(1.0, 0.0), lightAxis))
            ),
            max(
                fadeBottom * max(0.0, dot(vec2(0.0, -1.0), lightAxis)),
                fadeTop * max(0.0, dot(vec2(0.0, 1.0), lightAxis))
            )
        );
        float moonDirectionalEdge = max(
            max(
                fadeLeft * max(0.0, dot(vec2(-1.0, 0.0), -lightAxis)),
                fadeRight * max(0.0, dot(vec2(1.0, 0.0), -lightAxis))
            ),
            max(
                fadeBottom * max(0.0, dot(vec2(0.0, -1.0), -lightAxis)),
                fadeTop * max(0.0, dot(vec2(0.0, 1.0), -lightAxis))
            )
        );

        vec3 normalizedViewDirection = normalize(viewDirection);
        vec3 sunViewDirection = normalize(CirrusLightViewDirection);
        float sunAngularDistance = acos(clamp(dot(normalizedViewDirection, sunViewDirection), -1.0, 1.0));
        float moonAngularDistance = acos(clamp(dot(normalizedViewDirection, -sunViewDirection), -1.0, 1.0));
        float sunProximity = 1.0 - smoothstep(radians(5.0), radians(38.0), sunAngularDistance);
        float moonProximity = 1.0 - smoothstep(radians(5.0), radians(30.0), moonAngularDistance);
        float sunOcclusion = 1.0 - smoothstep(radians(7.0), radians(30.0), sunAngularDistance);
        float moonOcclusion = 1.0 - smoothstep(radians(5.0), radians(22.0), moonAngularDistance);
        float moonWeight = 1.0 - CirrusSunWeight;
        float celestialOcclusion = max(sunOcclusion * CirrusSunWeight, moonOcclusion * moonWeight);
        color.a = mix(color.a, 1.0, celestialOcclusion);

        float sunHighlight = sunDirectionalEdge * horizonAmount * CirrusSunWeight * sunProximity * 0.74;
        float moonHighlight = moonDirectionalEdge * horizonAmount * moonWeight * moonProximity * 0.12;
        vec3 sunEdgeColor = mix(CirrusLightColor, vec3(1.0), 0.70);
        vec3 moonEdgeColor = vec3(0.82, 0.89, 1.0);
        color.rgb = mix(color.rgb, sunEdgeColor, sunHighlight);
        color.rgb = mix(color.rgb, moonEdgeColor, moonHighlight);
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
