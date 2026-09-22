#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    vec4 clipPosition = ProjMat * viewPosition;
    clipPosition.z = -clipPosition.w;
    gl_Position = clipPosition;
    texCoord0 = UV0;
}
