#version 330

#moj_import <minecraft:globals.glsl>

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

float sdSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1.0e-8), 0.0, 1.0);
    return length(pa - ba * h);
}

float aaCore(float distance, float width) {
    float pixel = max(fwidth(distance), 0.0015);
    return 1.0 - smoothstep(width, width + pixel * 1.35, distance);
}

bool rayHit(vec2 p, vec2 a, vec2 b) {
    bool aboveA = a.y > p.y;
    bool aboveB = b.y > p.y;
    if (aboveA == aboveB) {
        return false;
    }
    float x = a.x + (b.x - a.x) * (p.y - a.y) / (b.y - a.y);
    return x > p.x;
}

float energy(vec2 p, vec2 a, vec2 b, float time, float offset) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float denom = max(dot(ba, ba), 1.0e-8);
    float t = clamp(dot(pa, ba) / denom, 0.0, 1.0);
    float distance = length(pa - ba * t);
    float onLine = 1.0 - smoothstep(0.0, 0.05, distance);
    float wave = fract(t * 1.2 - time * 0.62 + offset);
    float pulse = 1.0 - smoothstep(0.0, 0.16, abs(wave - 0.5));
    return onLine * pulse;
}

void main() {
    vec2 p = texCoord * 2.0 - 1.0;
    float radius = length(p);
    if (radius > 0.97) {
        discard;
    }

    float time = GameTime * 720.0;
    float breath = 0.78 + 0.22 * (0.5 + 0.5 * sin(time * 2.4));

    const float starR = 0.70;
    vec2 v0 = vec2(0.000000, 1.000000) * starR;
    vec2 v1 = vec2(-0.951057, 0.309017) * starR;
    vec2 v2 = vec2(-0.587785, -0.809017) * starR;
    vec2 v3 = vec2(0.587785, -0.809017) * starR;
    vec2 v4 = vec2(0.951057, 0.309017) * starR;

    float star = sdSegment(p, v0, v2);
    star = min(star, sdSegment(p, v2, v4));
    star = min(star, sdSegment(p, v4, v1));
    star = min(star, sdSegment(p, v1, v3));
    star = min(star, sdSegment(p, v3, v0));

    float pentagon = sdSegment(p, v0, v1);
    pentagon = min(pentagon, sdSegment(p, v1, v2));
    pentagon = min(pentagon, sdSegment(p, v2, v3));
    pentagon = min(pentagon, sdSegment(p, v3, v4));
    pentagon = min(pentagon, sdSegment(p, v4, v0));

    const float innerR = 0.2674;
    vec2 i0 = vec2(0.000000, -1.000000) * innerR;
    vec2 i1 = vec2(0.951057, -0.309017) * innerR;
    vec2 i2 = vec2(0.587785, 0.809017) * innerR;
    vec2 i3 = vec2(-0.587785, 0.809017) * innerR;
    vec2 i4 = vec2(-0.951057, -0.309017) * innerR;

    float inner = sdSegment(p, i0, i1);
    inner = min(inner, sdSegment(p, i1, i2));
    inner = min(inner, sdSegment(p, i2, i3));
    inner = min(inner, sdSegment(p, i3, i4));
    inner = min(inner, sdSegment(p, i4, i0));

    vec2 t0 = vec2(0.000000, 1.000000);
    vec2 t1 = vec2(-0.951057, 0.309017);
    vec2 t2 = vec2(-0.587785, -0.809017);
    vec2 t3 = vec2(0.587785, -0.809017);
    vec2 t4 = vec2(0.951057, 0.309017);
    float ticks = sdSegment(p, t0 * 0.755, t0 * 0.875);
    ticks = min(ticks, sdSegment(p, t1 * 0.755, t1 * 0.875));
    ticks = min(ticks, sdSegment(p, t2 * 0.755, t2 * 0.875));
    ticks = min(ticks, sdSegment(p, t3 * 0.755, t3 * 0.875));
    ticks = min(ticks, sdSegment(p, t4 * 0.755, t4 * 0.875));

    float circle = abs(radius - 0.82);
    float verts = length(p - v0);
    verts = min(verts, length(p - v1));
    verts = min(verts, length(p - v2));
    verts = min(verts, length(p - v3));
    verts = min(verts, length(p - v4));

    int crossings = 0;
    if (rayHit(p, v0, v2)) crossings++;
    if (rayHit(p, v2, v4)) crossings++;
    if (rayHit(p, v4, v1)) crossings++;
    if (rayHit(p, v1, v3)) crossings++;
    if (rayHit(p, v3, v0)) crossings++;
    float fill = float(crossings % 2) * (0.11 + 0.05 * breath) * (1.0 - smoothstep(0.18, 0.72, radius));

    float flow = energy(p, v0, v2, time, 0.00);
    flow = max(flow, energy(p, v2, v4, time, 0.20));
    flow = max(flow, energy(p, v4, v1, time, 0.40));
    flow = max(flow, energy(p, v1, v3, time, 0.60));
    flow = max(flow, energy(p, v3, v0, time, 0.80));

    float starCore = aaCore(star, 0.015);
    float starGlow = exp(-star * 26.0) * 0.64;
    float pentCore = aaCore(pentagon, 0.009) * 0.42;
    float pentGlow = exp(-pentagon * 24.0) * 0.22;
    float innerCore = aaCore(inner, 0.011) * 0.9;
    float innerGlow = exp(-inner * 30.0) * 0.4;
    float circCore = aaCore(circle, 0.012);
    float circGlow = exp(-circle * 28.0) * 0.5;
    float tickCore = aaCore(ticks, 0.008) * 0.7;
    float vertCore = 1.0 - smoothstep(0.0, 0.036, verts);
    float vertGlow = exp(-verts * 15.0) * 0.58;
    float heart = exp(-radius * radius * 7.2) * 0.18 * breath;

    float intensity = fill + starCore + starGlow + pentCore + pentGlow
            + innerCore + innerGlow + circCore + circGlow
            + tickCore + vertCore * 0.88 + vertGlow + heart + flow * 1.05;
    intensity *= vertexColor.a;
    if (intensity <= 0.003) {
        discard;
    }

    vec3 color = mix(vertexColor.rgb, vec3(1.0),
            clamp(starCore * 0.58 + flow * 0.78 + vertCore * 0.52 + circCore * 0.22, 0.0, 1.0));
    intensity = min(intensity, 1.4);
    fragColor = vec4(color * intensity, clamp(intensity, 0.0, 1.0));
}
