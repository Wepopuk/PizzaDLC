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
in float starN;
in float starM;

out vec4 fragColor;

float sdStar(vec2 p, float r, float n, float m) {
    float an = 3.141592653589793 / n;
    float en = 3.141592653589793 / m;
    vec2 acs = vec2(cos(an), sin(an));
    vec2 ecs = vec2(cos(en), sin(en));
    float bn = mod(atan(p.x, p.y), 2.0 * an) - an;
    p = length(p) * vec2(cos(bn), abs(sin(bn)));
    p -= r * acs;
    p += ecs * clamp(-dot(p, ecs), 0.0, r * acs.y / ecs.y);
    return length(p) * sign(p.x);
}

void main() {
    vec2 pixel = localCoord * rectSize;
    vec2 center = pixel - rectSize * 0.5;
    center.y = -center.y;
    float r = min(rectSize.x, rectSize.y) * 0.5;
    float d = sdStar(center, r, starN, clamp(starM, 2.0, starN));

    float aa = max(fwidth(d), 1e-5);
    float alpha = clamp(0.5 - d / aa, 0.0, 1.0);
    if (alpha <= 0.0) discard;

    vec4 color = vertexColor * ColorModulator;
    fragColor = vec4(color.rgb, color.a * alpha);
}
