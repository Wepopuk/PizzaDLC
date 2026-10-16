#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec4 colorLeft;
in vec4 colorRight;
in vec2 localCoord;
in vec2 rectSize;
in float cornerRadius;
in float cornerMask;

out vec4 fragColor;

float cornerR(vec2 c) {
    bool right = c.x > 0.0;
    bool top = c.y > 0.0;
    float bit = (!right && top) ? 1.0 : (right && top) ? 2.0 : (right && !top) ? 4.0 : 8.0;
    float on = mod(floor(cornerMask / bit), 2.0);
    return on > 0.5 ? cornerRadius : 0.0;
}

float roundedBoxSDF(vec2 center, vec2 halfSize, float r) {
    vec2 q = abs(center) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    vec2 pixel = localCoord * rectSize;
    vec2 center = pixel - rectSize * 0.5;
    center.y = -center.y;
    float r = cornerR(center);
    float d = roundedBoxSDF(center, rectSize * 0.5, r);

    float aa = max(fwidth(d), 1e-5);
    float alpha = clamp(0.5 - d / aa, 0.0, 1.0);
    if (alpha <= 0.0) discard;

    vec3 rgb = mix(vec3(1.0), colorRight.rgb, localCoord.x) * (1.0 - localCoord.y);
    vec4 color = vec4(rgb, 1.0) * ColorModulator;
    fragColor = vec4(color.rgb, color.a * alpha);
}
