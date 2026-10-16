#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

out vec4 vertexColor;
out vec4 colorB;
out vec2 localCoord;
out float radiusN;
out float thickN;
out float endDeg;
out float phase;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    localCoord = UV0;
    colorB = vec4(
        float(UV1.x & 0xFF),
        float((UV1.x >> 8) & 0xFF),
        float(UV1.y & 0xFF),
        float((UV1.y >> 8) & 0xFF)
    ) / 255.0;
    radiusN = float(UV2.x & 0xFF) / 255.0;
    thickN  = float((UV2.x >> 8) & 0xFF) / 255.0;
    endDeg  = float(UV2.y & 0xFF) * (360.0 / 255.0);
    phase   = float((UV2.y >> 8) & 0xFF) / 255.0;
}
