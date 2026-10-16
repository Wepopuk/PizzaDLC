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

const float PI = 3.14159265359;
const float TAU = 6.28318530718;

mat3 rotX(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotY(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
}

float hash12(vec2 value) {
    vec3 p = fract(vec3(value.xyx) * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    return mix(
        mix(hash12(cell), hash12(cell + vec2(1.0, 0.0)), local.x),
        mix(hash12(cell + vec2(0.0, 1.0)), hash12(cell + vec2(1.0)), local.x),
        local.y
    );
}

float fbm(vec2 point) {
    float result = 0.0;
    float weight = 0.5;
    for (int octave = 0; octave < 5; octave++) {
        result += noise(point) * weight;
        point = point * 2.03 + vec2(3.7, 1.9);
        weight *= 0.5;
    }
    return result;
}

vec2 seamlessNoisePoint(vec2 uv, float radius, float verticalScale, float seed) {
    float angle = uv.x * TAU;
    vec2 circle = vec2(cos(angle), sin(angle)) * radius;
    float vertical = uv.y * verticalScale;
    return circle + vec2(vertical + seed, vertical * 0.73 - seed * 0.61);
}

vec3 skyRay() {
    vec2 uv = gl_FragCoord.xy / max(uResolution, vec2(1.0));
    vec2 screen = uv * 2.0 - 1.0;
    float aspect = uResolution.x / max(uResolution.y, 1.0);
    float fov = tan(radians(uFov) * 0.5);
    vec3 viewRay = normalize(vec3(screen.x * fov * aspect, screen.y * fov, 1.0));
    return normalize(rotY(uCameraDir.x) * rotX(uCameraDir.y) * viewRay);
}

vec2 sphericalUv(vec3 direction) {
    return vec2(
        atan(direction.z, direction.x) / TAU,
        asin(clamp(direction.y, -1.0, 1.0)) / PI
    );
}

float starLayer(vec2 uv, float scale, float cutoff, vec2 offset, float time) {
    vec2 point = uv * vec2(scale, scale * 0.54) + offset;
    vec2 cell = floor(point);
    vec2 local = fract(point) - 0.5;
    float randomValue = hash12(cell + offset * 13.7);
    float exists = step(cutoff, randomValue);
    float radius = mix(0.022, 0.065, pow(randomValue, 12.0));
    float core = 1.0 - smoothstep(radius, radius + 0.042, length(local));
    float halo = 1.0 - smoothstep(radius * 1.5, radius * 3.0 + 0.02, length(local));
    float twinkle = 0.76 + 0.24 * sin(time * (0.8 + randomValue * 1.7)
            + randomValue * 31.0);
    return exists * (core + halo * 0.045) * twinkle;
}

float nebulaBand(vec2 uv, float center, float width, float seed, float time, float scale) {
    vec2 largePoint = seamlessNoisePoint(uv, 0.78 * scale, 5.0 * scale, seed);
    vec2 finePoint = seamlessNoisePoint(uv, 2.18 * scale, 13.5 * scale, seed * 1.71);
    float largeWarp = fbm(largePoint + vec2(0.0, time * 0.0025));
    float fineWarp = fbm(finePoint + vec2(0.0, -time * 0.004));
    float waveCycles = 2.0 + mod(floor(seed), 3.0);
    float waveCenter = center
            + sin(uv.x * TAU * waveCycles + seed * 2.0 + time * 0.006) * 0.025
            + (largeWarp - 0.5) * 0.13;
    float distanceToBand = abs(uv.y - waveCenter);
    float band = exp(-(distanceToBand * distanceToBand) / (width * width));

    vec2 patchPoint = seamlessNoisePoint(uv, 1.42 * scale, 8.0 * scale, seed * 4.2);
    float patchNoise = fbm(patchPoint + vec2(time * 0.001, time * 0.003));
    float patches = smoothstep(0.30, 0.64, patchNoise);
    float filaments = smoothstep(0.28, 0.82, fineWarp);
    return band * patches * mix(0.18, 1.0, filaments);
}

void main() {
    vec3 ray = skyRay();
    vec2 sphere = sphericalUv(ray);
    float time = uTime * uSpeed;
    float weather = clamp(uRain * 0.65 + uThunder, 0.0, 1.0);

    vec3 sky = vec3(0.0005, 0.0010, 0.0035);

    float starsNear = starLayer(sphere, 230.0, 0.965, vec2(7.0, 13.0), time);
    float starsFar = starLayer(sphere, 430.0, 0.982, vec2(31.0, 5.0), time * 0.73);
    float starVisibility = 1.0 - weather;
    sky += vec3(0.63, 0.72, 0.94) * starsFar * 0.36 * starVisibility;
    sky += vec3(0.82, 0.88, 1.00) * starsNear * 0.62 * starVisibility;

    vec3 themePrimary = mix(vec3(0.24, 0.40, 1.0), uColor.rgb, uThemeMix);
    vec3 themeSecondary = mix(vec3(0.43, 0.60, 1.0), uColor2.rgb, uThemeMix);
    vec3 themeCloud = mix(themePrimary, themeSecondary, 0.46);
    vec3 brightTheme = mix(themeCloud, vec3(0.88, 0.94, 1.0), 0.42);
    float cloudScale = mix(0.58, 1.05, clamp((uScale - 1.0) / 19.0, 0.0, 1.0));
    float lowestCloud = nebulaBand(sphere, -0.34, 0.060, 11.7, time, cloudScale);
    float lowerCloud = nebulaBand(sphere, -0.18, 0.074, 1.4, time, cloudScale);
    float middleCloud = nebulaBand(sphere, 0.015, 0.088, 4.8, time, cloudScale);
    float upperCloud = nebulaBand(sphere, 0.20, 0.072, 8.2, time, cloudScale);
    float highestCloud = nebulaBand(sphere, 0.35, 0.055, 15.3, time, cloudScale);
    float cloud = clamp(lowestCloud * 0.58 + lowerCloud * 0.84 + middleCloud
            + upperCloud * 0.78 + highestCloud * 0.52, 0.0, 1.0);

    float colorVariation = fbm(seamlessNoisePoint(sphere, 2.05, 11.0, 17.0)
            + vec2(time * 0.002, 0.0));
    vec3 cloudColor = mix(themePrimary, themeSecondary, colorVariation);
    float cloudStrength = 0.24 + uIntensity * 0.64;
    sky += cloudColor * cloud * cloudStrength;

    float coreNoise = fbm(seamlessNoisePoint(sphere, 4.1, 20.0, 43.0)
            + vec2(-time * 0.003, 0.0));
    float cloudCore = pow(smoothstep(0.70, 0.91, coreNoise) * cloud, 2.2);
    float softCore = pow(smoothstep(0.54, 0.82, coreNoise) * cloud, 2.0);
    sky += themeCloud * softCore * (0.12 + uIntensity * 0.20);
    sky += brightTheme * cloudCore * (0.58 + uIntensity * 0.55);

    sky *= 1.0 - weather * 0.36;
    sky = vec3(1.0) - exp(-max(sky, vec3(0.0)) * 1.12);
    fragColor = vec4(clamp(sky, 0.0, 1.0), uAlpha);
}
