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
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = p * 2.0 + vec2(1.7, 3.1);
        a *= 0.5;
    }
    return v;
}

vec3 skyRay() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / uResolution.y;
    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    return normalize(rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV);
}

float roundStars(vec3 dir, float t) {
    vec3 p = dir * 70.0;
    vec3 ip = floor(p);
    vec3 fp = fract(p) - 0.5;
    float rnd = hash13(ip);
    float has = step(0.965, rnd);
    float d = length(fp);
    float dot = smoothstep(0.22, 0.0, d);
    float tw = 0.55 + 0.45 * sin(t * 2.5 + rnd * 6.2831);
    return has * dot * tw;
}

void main() {
    vec3 ray = skyRay();
    float t = uTime * uSpeed;
    float height = ray.y;
    float h = smoothstep(-0.45, 0.95, height);
    float night = clamp(uNight, 0.0, 1.0);

    vec3 themeTint = mix(uColor.rgb, uColor2.rgb, 0.5);

    vec3 dayZenith  = mix(vec3(0.28, 0.62, 1.0), uColor.rgb, 0.22 * uThemeMix);
    vec3 dayHorizon = mix(vec3(1.0, 0.72, 0.36), uColor2.rgb, 0.18 * uThemeMix);
    vec3 nightZenith  = mix(vec3(0.015, 0.03, 0.09), themeTint * 0.2, uThemeMix);
    vec3 nightHorizon = mix(vec3(0.05, 0.08, 0.18), themeTint * 0.3, uThemeMix);
    vec3 zenith  = mix(dayZenith, nightZenith, night);
    vec3 horizon = mix(dayHorizon, nightHorizon, night);
    vec3 sky = mix(horizon, zenith, h);

    vec3 sunDir = normalize(vec3(-0.42, 0.34, 0.84));
    float sunDot = max(dot(ray, sunDir), 0.0);
    vec3 dayLight = vec3(1.0, 0.88, 0.55) * pow(sunDot, 14.0) * 0.58
                  + vec3(1.0, 0.96, 0.82) * pow(sunDot, 900.0) * 2.4;
    vec3 nightLight = vec3(0.7, 0.78, 0.95) * pow(sunDot, 40.0) * 0.4
                    + vec3(0.95, 0.97, 1.0) * pow(sunDot, 2200.0) * 2.2;
    sky += mix(dayLight, nightLight, night);

    float stars = roundStars(ray, t) * smoothstep(0.02, 0.4, height);
    sky += vec3(0.9, 0.93, 1.0) * stars * night * (1.0 - clamp(uRain + uThunder, 0.0, 1.0));

    float scale = max(uScale, 1.0);
    vec2 cc = vec2(ray.x, ray.z) * scale * 1.6 + vec2(t * 0.02, t * 0.008);
    float cloudBase = fbm(cc * 0.42 + vec2(0.0, 4.0));
    float cloudDetail = fbm(cc * 0.9 + vec2(8.5, 1.7));
    float cloudBand = smoothstep(-0.05, 0.55, height) * smoothstep(0.85, 0.12, height);
    float clouds = smoothstep(0.48, 0.72, cloudBase + cloudDetail * 0.28) * cloudBand;
    clouds *= 1.0 - clamp(uRain + uThunder, 0.0, 1.0) * 0.7;
    vec3 dayCloud = mix(vec3(1.0, 0.92, 0.82), vec3(0.88, 0.94, 1.0), h);
    vec3 nightCloud = mix(vec3(0.10, 0.13, 0.22), vec3(0.16, 0.20, 0.32), h);
    vec3 cloudColor = mix(dayCloud, nightCloud, night);
    sky = mix(sky, cloudColor, clouds * mix(0.78, 0.62, night));

    float haze = smoothstep(-0.6, 0.08, height) * (1.0 - smoothstep(0.08, 0.45, height));
    vec3 dayHaze = mix(vec3(1.0, 0.55, 0.22), themeTint, uThemeMix);
    vec3 nightHaze = mix(vec3(0.14, 0.20, 0.40), themeTint * 0.5, uThemeMix);
    sky += mix(dayHaze, nightHaze, night) * haze * (0.22 + uIntensity * 14.0);

    vec3 overcast = mix(vec3(0.34, 0.38, 0.48), vec3(0.06, 0.08, 0.14), night);
    sky = mix(sky, overcast, clamp(uRain + uThunder, 0.0, 1.0) * 0.35);

    fragColor = vec4(clamp(sky, 0.0, 1.0), uAlpha);
}
