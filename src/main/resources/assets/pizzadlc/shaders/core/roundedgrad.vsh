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

out vec4 colorLeft;
out vec4 colorRight;
out vec2 localCoord;
out vec2 rectSize;
out float cornerRadius;
out float cornerMask;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    colorLeft = Color;
    colorRight = vec4(
        float(UV1.x & 0xFF),
        float((UV1.x >> 8) & 0xFF),
        float(UV1.y & 0xFF),
        float((UV1.y >> 8) & 0xFF)
    ) / 255.0;
    localCoord = UV0;
    rectSize = vec2(float(UV2.x & 0xFF), float((UV2.x >> 8) & 0xFF));
    cornerRadius = float(UV2.y & 0xFF);
    cornerMask = float((UV2.y >> 8) & 0xFF);
}
