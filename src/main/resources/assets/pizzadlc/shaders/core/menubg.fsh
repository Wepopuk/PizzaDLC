#version 330

in vec2 vLocal;
in vec2 iResolution;
in float iTime;

out vec4 outColor;

float hash(vec2 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * (p.x + p.y));
}
float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return mix(mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}
float fbm(vec2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 5; i++) { v += a * vnoise(p); p = p * 2.0 + 19.0; a *= 0.5; }
    return v;
}

vec4 lookup(float x, float y, float seed) {
    vec2 p = vec2(x, y) / iResolution.xy * 4.0 + vec2(seed, seed * 0.7);
    p += 0.5 * vec2(fbm(p), fbm(p + 4.7));
    return vec4(vec3(fbm(p)), 1.0);
}

float rayStrength(vec2 raySource, vec2 rayRefDirection, vec2 coord, float seedA, float seedB, float speed) {
    vec2 sourceToCoord = coord - raySource;
    float cosAngle = dot(normalize(sourceToCoord), rayRefDirection);
    return clamp(
        (0.45 + 0.15 * sin(cosAngle * seedA + iTime * speed)) +
        (0.3 + 0.2 * cos(-cosAngle * seedB + iTime * speed)),
        0.0, 1.0) *
        clamp((iResolution.x - length(sourceToCoord)) / iResolution.x, 0.5, 1.0);
}

float bubbleStrength(vec2 startPos, vec2 waveOffset, float radius, float speed, vec2 coord) {
    vec2 curPos = vec2(
        mod(startPos.x + waveOffset.x * 0.5, iResolution.x + radius * 2.0) - radius,
        mod(waveOffset.y - iTime * speed, iResolution.y + radius * 2.0) - radius);
    return 1.0 - smoothstep(0.0, radius, length(coord - curPos));
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = fragCoord.xy / iResolution.xy;
    uv.y = 1.0 - uv.y;
    vec2 coord = vec2(fragCoord.x, iResolution.y - fragCoord.y);

    float offsetX = 26.77311 * iTime;
    float offsetY = 0.0;

    vec4 tex1 = lookup(coord.x + offsetX, coord.y + offsetY, 0.0);
    vec4 tex2 = lookup(coord.x * 2.0 + offsetX * 1.6, coord.y * 2.0 + offsetY * 1.5, 5.3);
    vec4 tex3 = lookup(coord.x * 3.0 + offsetX * 1.3, coord.y * 3.0 + offsetY * 1.2, 11.7);
    vec4 tex4 = lookup(coord.x * 0.5 + offsetX * 0.8, coord.y * 0.5 + offsetY * 0.9, 17.1);

    vec2 rayPos1 = vec2(iResolution.x * 0.7, iResolution.y * -0.4);
    vec2 rayRefDir1 = normalize(vec2(1.0, -0.116));
    float raySeedA1 = 36.2214;
    float raySeedB1 = 21.11349;
    float raySpeed1 = 1.5;

    vec2 rayPos2 = vec2(iResolution.x * 0.8, iResolution.y * -0.6);
    vec2 rayRefDir2 = normalize(vec2(1.0, 0.241));
    float raySeedA2 = 22.39910;
    float raySeedB2 = 18.0234;
    float raySpeed2 = 1.1;

    vec4 rays1 = vec4(1.0) * rayStrength(rayPos1, rayRefDir1, coord, raySeedA1, raySeedB1, raySpeed1);
    vec4 rays2 = vec4(1.0) * rayStrength(rayPos2, rayRefDir2, coord, raySeedA2, raySeedB2, raySpeed2);

    float bubbleScale = iResolution.x / 600.0;

    vec4 bubble1 = vec4(1.0) * bubbleStrength(vec2(0.0, 0.0),     vec2(offsetX * 0.2312, 0.0),               20.0 * bubbleScale, 60.0, coord);
    vec4 bubble2 = vec4(1.0) * bubbleStrength(vec2(40.0, 400.0),  vec2(offsetX * -0.06871, offsetY * 0.301),  7.0 * bubbleScale, 25.0, coord);
    vec4 bubble3 = vec4(1.0) * bubbleStrength(vec2(300.0, 70.0),  vec2(offsetX * 0.19832, offsetY * 0.1351), 14.0 * bubbleScale, 45.0, coord);
    vec4 bubble4 = vec4(1.0) * bubbleStrength(vec2(500.0, 280.0), vec2(offsetX * -0.0993, offsetY * -0.2654),12.0 * bubbleScale, 32.0, coord);
    vec4 bubble5 = vec4(1.0) * bubbleStrength(vec2(400.0, 140.0), vec2(offsetX * 0.2231, offsetY * 0.0111),  10.0 * bubbleScale, 28.0, coord);
    vec4 bubble6 = vec4(1.0) * bubbleStrength(vec2(200.0, 360.0), vec2(offsetX * 0.0693, offsetY * -0.3567),  5.0 * bubbleScale, 12.0, coord);
    vec4 bubble7 = vec4(1.0) * bubbleStrength(vec2(0.0, 0.0),     vec2(offsetX * -0.32301, offsetY * 0.2349),16.0 * bubbleScale, 51.0, coord);
    vec4 bubble8 = vec4(1.0) * bubbleStrength(vec2(130.0, 23.0),  vec2(offsetX * 0.1393, offsetY * -0.4013),  8.0 * bubbleScale, 24.0, coord);

    fragColor =
        tex1 * 0.45 + tex2 * 0.25 + tex3 * 0.15 + tex4 * 0.1 +
        rays1 * 0.5 + rays2 * 0.4 +
        bubble1 * 0.25 + bubble2 * 0.1 + bubble3 * 0.18 + bubble4 * 0.13 +
        bubble5 * 0.15 + bubble6 * 0.05 + bubble7 * 0.12 + bubble8 * 0.11;

    float brightness = 1.0 - (coord.y / iResolution.y);
    fragColor.x *= 0.2 + (brightness * 0.8);
    fragColor.y *= 0.3 + (brightness * 0.7);
    fragColor.z *= 0.4 + (brightness * 0.6);
}

void main() {
    vec2 fragCoord = vec2(vLocal.x, 1.0 - vLocal.y) * iResolution;
    vec4 col;
    mainImage(col, fragCoord);
    outColor = vec4(col.rgb, 1.0);
}
