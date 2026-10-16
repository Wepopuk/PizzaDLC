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

out vec4 colorTop;
out vec4 colorBottom;
out vec2 localCoord;
out vec2 rectSize;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    colorTop = Color;
    colorBottom = vec4(
        float(UV1.x & 0xFF),
        float((UV1.x >> 8) & 0xFF),
        float(UV1.y & 0xFF),
        float((UV1.y >> 8) & 0xFF)
    ) / 255.0;
    localCoord = UV0;
    rectSize = vec2(float(UV2.x), float(UV2.x));
}
