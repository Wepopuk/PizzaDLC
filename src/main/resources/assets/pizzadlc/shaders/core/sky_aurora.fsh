#version 330

layout(std140) uniform SkyConfig {
    vec2 uResolution;
    float uTime;
    float uFov;
    vec2 uCameraDir;
    float uScale;
    float uIntensity;
    float uAlpha;
    float uSpeed;
    vec4 uColor;
    vec4 uColor2;
    float uNight;
    float uRain;
    float uThunder;
    float uThemeMix;
};

out vec4 fragColor;

mat3 rotX(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat3(1.0, 0.0, 0.0,
                0.0, c, s,
                0.0, -s, c);
}

mat3 rotY(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat3(c, 0.0, s,
                0.0, 1.0, 0.0,
                -s, 0.0, c);
}

float hash12(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float n00 = hash12(i);
    float n10 = hash12(i + vec2(1.0, 0.0));
    float n01 = hash12(i + vec2(0.0, 1.0));
    float n11 = hash12(i + vec2(1.0, 1.0));
    return mix(mix(n00, n10, u.x), mix(n01, n11, u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.02 + vec2(7.1, -3.4);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec2 resolution = max(uResolution, vec2(1.0));
    vec2 screen = gl_FragCoord.xy / resolution * 2.0 - 1.0;
    screen.x *= resolution.x / resolution.y;
    float tanFov = tan(radians(uFov) * 0.5);
    vec3 rd = normalize(vec3(screen * tanFov, 1.0));
    rd = normalize(rotY(uCameraDir.x) * rotX(uCameraDir.y) * rd);

    float horizon = clamp(rd.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 nightA = vec3(0.01, 0.03, 0.09);
    vec3 nightB = vec3(0.00, 0.00, 0.02);
    vec3 col = mix(nightB, nightA, smoothstep(0.0, 1.0, horizon));

    float t = uTime * uSpeed * 0.45;
    float skyScale = max(0.35, uScale);
    vec2 uv = rd.xz * (4.8 * skyScale);
    uv.y += rd.y * (2.2 * skyScale);

    float warp = fbm(uv * 1.1 + vec2(t * 0.17, -t * 0.05));
    float waves = sin((uv.x + warp * 1.7) * 6.0 + t * 1.8) * 0.5 + 0.5;
    float veil = smoothstep(0.25, 1.0, fbm(uv * 2.4 + vec2(0.0, t * 0.11)));
    float bandMask = smoothstep(0.05, 0.75, rd.y) * (1.0 - smoothstep(0.85, 1.0, rd.y));
    float aurora = pow(waves * veil, 1.25) * bandMask;

    vec3 tint = clamp(uColor.rgb, 0.0, 1.0);
    vec3 auroraA = mix(vec3(0.10, 0.95, 0.55), tint, 0.30);
    vec3 auroraB = vec3(0.20, 0.70, 1.00);
    vec3 auroraCol = mix(auroraA, auroraB, smoothstep(0.25, 0.95, waves));
    col += auroraCol * aurora * 1.05;

    vec2 starUv = rd.xz / max(0.05, abs(rd.y) + 0.35) * 140.0;
    vec2 sid = floor(starUv);
    vec2 sfract = fract(starUv) - 0.5;
    float sh = hash12(sid);
    float tw = 0.75 + 0.25 * sin(t * 6.0 + sh * 40.0);
    float stars = smoothstep(0.9965, 0.9998, sh) * smoothstep(0.30, 0.0, length(sfract));
    col += vec3(0.8, 0.9, 1.0) * stars * tw * (0.6 + 0.4 * horizon);

    col = col / (col + vec3(0.9));
    col = pow(clamp(col, 0.0, 1.0), vec3(0.92));
    fragColor = vec4(col, uColor.a);
}
