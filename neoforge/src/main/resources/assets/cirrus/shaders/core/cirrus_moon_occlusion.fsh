#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 moon = texture(Sampler0, texCoord0) * ColorModulator;
    float brightness = max(max(moon.r, moon.g), moon.b);
    gl_FragDepth = brightness >= 0.24 ? 0.0 : gl_FragCoord.z;
    fragColor = moon;
}
