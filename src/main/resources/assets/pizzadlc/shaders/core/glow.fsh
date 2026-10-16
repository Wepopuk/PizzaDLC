#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec4 vertexColor;
in vec2 localCoord;
in vec2 rectSize;
in float radius;
in float spread;

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 halfSize, float r) {
    vec2 q = abs(center) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 expanded = rectSize + 2.0 * spread;
    vec2 pixel = localCoord * expanded;
    vec2 center = pixel - expanded * 0.5;
    float r = abs(radius);
    float d = roundedBoxSDF(center, rectSize * 0.5, r);
    float dist = radius < 0.0 ? abs(d) : max(d, 0.0);
    float glow = 1.0 - smoothstep(0.0, spread, dist);
    if (glow <= 0.0) discard;
    vec4 color = vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * glow);
}
