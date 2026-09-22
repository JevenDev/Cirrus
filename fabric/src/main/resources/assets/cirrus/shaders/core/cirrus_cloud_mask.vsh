#version 330

layout(std140) uniform CirrusMatrices {
    mat4 ModelViewMat;
    mat4 ProjMat;
};

layout(std140) uniform CirrusParams {
    float CirrusRainCloudCoverage;
    float CirrusThunderCloudCoverage;
    float CirrusLayerOpacity;
};


in vec3 Position;
in vec2 UV0;


out vec2 texCoord0;

void main() {
    vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
    vec4 clipPosition = ProjMat * viewPosition;
    clipPosition.z = -clipPosition.w;
    gl_Position = clipPosition;
    texCoord0 = UV0;
}
