#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec4 colorTop;
in vec4 colorBottom;
in vec2 localCoord;
in vec2 rectSize;

out vec4 fragColor;

const float GY = -0.05;
const vec2 AP = vec2( 0.00, -0.34);
const vec2 SL = vec2(-0.28,  GY);
const vec2 SR = vec2( 0.28,  GY);
const vec2 BM = vec2( 0.00,  0.36);

float segDist(vec2 p, vec2 a, vec2 b) {
    vec2 e = b - a, w = p - a;
    return length(w - e * clamp(dot(w, e) / dot(e, e), 0.0, 1.0));
}

float sdKite(vec2 p) {
    vec2 v[4];
    v[0] = AP; v[1] = SR; v[2] = BM; v[3] = SL;
    float d = dot(p - v[0], p - v[0]);
    float s = 1.0;
    for (int i = 0, j = 3; i < 4; j = i, i++) {
        vec2 e = v[j] - v[i];
        vec2 w = p - v[i];
        vec2 b = w - e * clamp(dot(w, e) / dot(e, e), 0.0, 1.0);
        d = min(d, dot(b, b));
        bvec3 c = bvec3(p.y >= v[i].y, p.y < v[j].y, e.x * w.y > e.y * w.x);
        if (all(c) || all(not(c))) s = -s;
    }
    return s * sqrt(d);
}

void main() {
    vec2 p = localCoord - 0.5;

    float d = sdKite(p);
    float aa = max(fwidth(d), 1e-5);
    float alpha = clamp(0.5 - d / aa, 0.0, 1.0);
    if (alpha <= 0.0) discard;

    bool crown = p.y < GY;
    bool leftF = p.x < 0.0;
    vec3 base  = crown ? colorTop.rgb : colorBottom.rgb;
    float sh   = crown ? (leftF ? 1.12 : 0.94) : (leftF ? 0.98 : 0.80);
    vec3 col   = clamp(base * sh, 0.0, 1.0);

    float px = 1.0 / max(min(rectSize.x, rectSize.y), 1.0);
    float ew = px * 0.9;
    float e  = min(segDist(p, AP, BM), segDist(p, SL, SR));
    float edge = 1.0 - smoothstep(ew * 0.5, ew * 1.5, e);
    col = mix(col, min(colorTop.rgb * 1.7, vec3(1.0)), edge * 0.35);

    float g = length((p - vec2(-0.05, -0.18)) * vec2(1.2, 1.0));
    col = mix(col, vec3(1.0), (1.0 - smoothstep(0.03, 0.09, g)) * 0.5);

    float a = (crown ? colorTop.a : colorBottom.a) * alpha;
    fragColor = vec4(col * ColorModulator.rgb, a * ColorModulator.a);
}
