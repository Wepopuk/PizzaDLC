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

float hash(vec2 point) {
    point = fract(point * vec2(123.34, 345.45));
    point += dot(point, point + 34.345);
    return fract(point.x * point.y);
}

float noise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);

    float a = hash(cell);
    float b = hash(cell + vec2(1.0, 0.0));
    float c = hash(cell + vec2(0.0, 1.0));
    float d = hash(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm(vec2 point) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int index = 0; index < 5; index++) {
        value += noise(point) * amplitude;
        point = point * 2.03 + vec2(13.1, 7.7);
        amplitude *= 0.5;
    }
    return value;
}

float ridged(vec2 point) {
    float value = 0.0;
    float amplitude = 0.55;
    for (int index = 0; index < 4; index++) {
        float ridge = 1.0 - abs(noise(point) * 2.0 - 1.0);
        value += ridge * amplitude;
        point = point * 2.15 + vec2(4.7, 9.2);
        amplitude *= 0.5;
    }
    return value;
}

mat3 rotateX(float angle) {
    float cosine = cos(angle);
    float sine = sin(angle);
    return mat3(
        1.0, 0.0, 0.0,
        0.0, cosine, sine,
        0.0, -sine, cosine
    );
}

mat3 rotateY(float angle) {
    float cosine = cos(angle);
    float sine = sin(angle);
    return mat3(
        cosine, 0.0, sine,
        0.0, 1.0, 0.0,
        -sine, 0.0, cosine
    );
}

void main() {
    vec2 uv = gl_FragCoord.xy / max(uResolution, vec2(1.0));
    vec2 screenPosition = uv * 2.0 - 1.0;
    float aspect = uResolution.x / max(uResolution.y, 1.0);
    float tangent = tan(radians(uFov) * 0.5);
    vec3 viewRay = normalize(vec3(
        screenPosition.x * tangent * aspect,
        screenPosition.y * tangent,
        1.0
    ));
    vec3 worldRay = normalize(
        rotateY(uCameraDir.x) * rotateX(uCameraDir.y) * viewRay
    );

    vec3 basisX = normalize(vec3(0.74, 0.18, 0.65));
    vec3 basisY = normalize(vec3(-0.22, 0.96, 0.18));
    vec2 flow = vec2(dot(worldRay, basisX), dot(worldRay, basisY))
            * max(uScale, 0.001) * 0.8;
    float animation = uTime * uSpeed;
    vec2 drift = vec2(animation * 0.22, animation * 0.15);

    vec2 warp = vec2(
        fbm(flow * 0.85 + drift * 0.55 + vec2(0.0, 4.1)),
        fbm(flow * 0.80 - drift * 0.42 + vec2(3.7, 1.8))
    );
    vec2 warpedFlow = flow + (warp - 0.5) * 1.7;

    float mist = fbm(warpedFlow * 0.70 - drift * 0.18 + vec2(4.2, 8.1));
    float diagonal1 = warpedFlow.x * 1.02 + warpedFlow.y * 0.38 + animation * 0.21;
    float diagonal2 = warpedFlow.x * -0.58 + warpedFlow.y * 1.10 - animation * 0.13;

    float band1 = 1.0 - abs(sin(diagonal1 * 1.85 + mist * 4.8));
    float band2 = 1.0 - abs(sin(diagonal2 * 1.45 - mist * 3.2));
    band1 = pow(clamp(band1, 0.0, 1.0), 4.6);
    band2 = pow(clamp(band2, 0.0, 1.0), 5.1);

    float veins = ridged(warpedFlow * 1.90 + vec2(mist * 2.7, mist * 1.9) - drift * 0.55);
    veins = pow(clamp(veins, 0.0, 1.0), 2.6);
    float micro = ridged(warpedFlow * 3.6 - vec2(7.1, 2.6) + drift * 0.35);
    micro = pow(clamp(micro, 0.0, 1.0), 5.8);

    float energy = clamp(
        mist * 0.24 + band1 * 0.70 + band2 * 0.40
        + veins * 0.84 + micro * 0.28,
        0.0,
        1.0
    );
    float contrast = mix(0.75, 1.4, clamp(uIntensity, 0.0, 1.0));
    float core = smoothstep(0.16, 0.98, energy * contrast);
    float glow = pow(clamp(max(veins, band1), 0.0, 1.0), 1.35) * 0.55
            + micro * 0.22;
    float pulse = 0.97 + sin(animation * 1.35) * 0.03;
    vec3 color = uColor.rgb * (0.30 + mist * 0.18 + core * 0.82 + glow * 0.45) * pulse;

    fragColor = vec4(clamp(color, 0.0, 1.0), uAlpha);
}
