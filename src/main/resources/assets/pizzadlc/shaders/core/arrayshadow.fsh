#version 330

layout(std140) uniform ShadowConfig {
    vec4 uMeta;
    vec4 uRadii;
    vec4 uRects[32];
};

out vec4 fragColor;

float sdRoundBoxPC(vec2 p, vec2 b, vec4 rad) {
    float rL = (p.y < 0.0) ? rad.x : rad.w;
    float rR = (p.y < 0.0) ? rad.y : rad.z;
    float r  = (p.x > 0.0) ? rR : rL;
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    int count   = int(uMeta.x + 0.5);
    float spread = uMeta.y;
    float height = uMeta.z;
    float alpha  = uMeta.w;

    vec2 px = vec2(gl_FragCoord.x, height - gl_FragCoord.y);

    float sdf = 1e9;
    for (int i = 0; i < 32; i++) {
        if (i >= count) break;
        vec2 tl = uRects[i].xy;
        vec2 sz = uRects[i].zw;
        vec2 c  = tl + sz * 0.5;
        sdf = min(sdf, sdRoundBoxPC(px - c, sz * 0.5, uRadii));
    }

    float cov = 1.0 - smoothstep(0.0, spread, max(sdf, 0.0));
    if (cov <= 0.0) discard;
    fragColor = vec4(0.0, 0.0, 0.0, cov * alpha);
}
