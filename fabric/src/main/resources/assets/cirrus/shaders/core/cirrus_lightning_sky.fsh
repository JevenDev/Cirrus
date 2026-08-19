#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    vec3 CirrusLightningSkyDirection;
    float CirrusLightningSkyIntensity;
};



in vec3 worldDirection;

out vec4 fragColor;

void main() {
    vec3 direction = normalize(worldDirection);
    vec3 flashDirection = normalize(CirrusLightningSkyDirection);
    float alignment = clamp(dot(direction, flashDirection), -1.0, 1.0);

    float directionalGlow = pow(clamp(alignment * 0.5 + 0.5, 0.0, 1.0), 3.5);
    float nearbyGlow = pow(max(alignment, 0.0), 10.0);
    float skyFade = smoothstep(-0.14, 0.10, direction.y);
    float intensity = clamp(CirrusLightningSkyIntensity, 0.0, 1.0) * skyFade;
    float alpha = intensity * (0.006 + directionalGlow * 0.035 + nearbyGlow * 0.014);
    if (alpha < 0.001) {
        discard;
    }

    vec3 outerColor = vec3(0.40, 0.49, 0.80);
    vec3 innerColor = vec3(0.78, 0.87, 1.0);
    vec3 color = mix(outerColor, innerColor, directionalGlow * 0.72 + nearbyGlow * 0.28);
    fragColor = vec4(color, alpha);
}
