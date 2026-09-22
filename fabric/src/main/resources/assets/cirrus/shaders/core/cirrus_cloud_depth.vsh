#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord0;
out float vertexAlpha;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * pos;
    texCoord0 = UV0;
    vertexAlpha = Color.a;
}
