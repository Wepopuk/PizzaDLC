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
in vec4 cornerRadii;
in float strokeThick;

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 halfSize, vec4 radius) {
    vec2 halfR = (center.x > 0.0) ? radius.yz : radius.xw;
    float r = (center.y > 0.0) ? halfR.x : halfR.y;
    vec2 q = abs(center) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 pixel = localCoord * rectSize;
    vec2 center = pixel - rectSize * 0.5;
    center.y = -center.y;
    float d = roundedBoxSDF(center, rectSize * 0.5, cornerRadii);

    float aa = fwidth(d);
    float outer = clamp(0.5 - d / aa, 0.0, 1.0);
    float inner = clamp(0.5 - (d + strokeThick) / aa, 0.0, 1.0);
    float alpha = outer - inner;
    if (alpha <= 0.0) discard;

    vec4 color = vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * alpha);
}
