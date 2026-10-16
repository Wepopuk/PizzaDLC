#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 localCoord;
in float rectSize;
in float cornerRadius;
in vec2 texCoord0;

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 halfSize, float r) {
    vec2 q = abs(center) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 pixel = localCoord * vec2(rectSize);
    vec2 center = pixel - vec2(rectSize) * 0.5;
    float d = roundedBoxSDF(center, vec2(rectSize) * 0.5, cornerRadius);
    float aa = fwidth(d);
    float mask = clamp(0.5 - d / aa, 0.0, 1.0);
    if (mask <= 0.0) discard;

    vec4 tex = texture(Sampler0, texCoord0);
    vec4 tint = vertexColor * ColorModulator;
    fragColor = vec4((tex * tint).rgb, tex.a * tint.a * mask);
}
