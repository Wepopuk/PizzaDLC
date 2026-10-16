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
in float dirUp;
in float strokeThick;

out vec4 fragColor;

float segDist(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

void main() {
    vec2 uv = localCoord;
    vec2 A, B, C;
    if (dirUp > 1.5) {
        A = vec2(0.20, 0.52) * rectSize;
        B = vec2(0.42, 0.74) * rectSize;
        C = vec2(0.80, 0.26) * rectSize;
    } else {
        if (dirUp > 0.5) uv.y = 1.0 - uv.y;
        A = vec2(0.12, 0.32) * rectSize;
        B = vec2(0.50, 0.68) * rectSize;
        C = vec2(0.88, 0.32) * rectSize;
    }
    vec2 p = uv * rectSize;
    float d = min(segDist(p, A, B), segDist(p, B, C));

    float aa = fwidth(d);
    float alpha = clamp(0.5 + (strokeThick * 0.5 - d) / aa, 0.0, 1.0);
    if (alpha <= 0.0) discard;

    vec4 color = vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * alpha);
}
