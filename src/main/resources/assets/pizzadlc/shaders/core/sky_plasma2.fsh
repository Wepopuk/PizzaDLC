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
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0,
                0.0,   c,   s,
                0.0,  -s,   c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(  c, 0.0,   s,
                0.0, 1.0, 0.0,
                 -s, 0.0,   c);
}

float hash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash(i), hash(i + vec3(1.0, 0.0, 0.0)), u.x),
            mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), u.x), u.y),
        mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), u.x),
            mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), u.x), u.y),
        u.z);
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int i = 0; i < 5; ++i) {
        value += amplitude * valueNoise(p);
        p = p * 2.03 + vec3(17.3, 9.1, 13.7);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(uResolution, vec2(1.0));
    vec2 screen = uv * 2.0 - 1.0;
    float aspect = uResolution.x / max(uResolution.y, 1.0);
    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayView = normalize(vec3(screen.x * tanV * aspect, screen.y * tanV, 1.0));
    vec3 ray = normalize(rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayView);

    vec3 primary = clamp(uColor.rgb, 0.0, 1.0);
    vec3 secondary = clamp(uColor2.rgb, 0.0, 1.0);
    vec3 softAccent = mix(primary, secondary, 0.28);
    float spatialScale = mix(1.1, 4.8, clamp((uScale - 1.0) / 19.0, 0.0, 1.0));
    float t = uTime * uSpeed * 0.012;
    float spin = uTime * uSpeed * 0.085;
    vec3 flowRay = rotY(spin) * ray;
    flowRay = rotY(flowRay.y * 1.35 + sin(uTime * uSpeed * 0.045) * 0.18) * flowRay;
    vec3 contourRay = rotY(-spin * 0.58 + ray.y * 0.82) * ray;

    float smokeA = fbm(flowRay * spatialScale * 1.9 + vec3(t, -t * 0.6, t * 0.35));
    float smokeB = fbm(flowRay * spatialScale * 3.1
                     + vec3(-t * 0.7, -t * 0.4, t * 0.5) + 41.0);
    float smoke = smoothstep(0.42, 0.98, smokeA * 0.72 + smokeB * 0.28);
    float upperSky = smoothstep(-0.65, 0.8, ray.y);
    float smokeMask = mix(0.48, 1.0, upperSky);

    vec3 base = mix(vec3(0.006) + primary * 0.004,
                    vec3(0.009) + softAccent * 0.016, upperSky);
    vec3 smokeColor = mix(primary * 0.16, softAccent * 0.25,
                          smoothstep(0.3, 1.0, smoke));
    base += smokeColor * smoke * smokeMask * 0.9;

    float field = fbm(contourRay * spatialScale * 2.6 + vec3(7.7, 3.3, 5.1) + t * 0.15);
    float bands = abs(fract(field * 9.0) - 0.5);
    float contour = 1.0 - smoothstep(0.02, 0.075, bands);
    float contourMask = smoothstep(0.15, 0.95, abs(ray.y));
    base += contour * contourMask * softAccent * 0.105;

    float energy = mix(0.72, 1.48,
            clamp((uIntensity - 0.1) / 0.9, 0.0, 1.0));
    fragColor = vec4(clamp(base * energy, 0.0, 1.0), clamp(uAlpha, 0.0, 1.0));
}
