#version 330

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

in vec2 texCoord;
out vec4 outColor;

#define iResolution ScreenSize
#define iTime (mod(GameTime, 600.0))

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

void main() {
    vec2 fragCoord = texCoord * iResolution;
    vec2 uv = fragCoord.xy / iResolution.xy;
    uv.y = 1.0 - uv.y;
    vec2 coord = vec2(fragCoord.x, iResolution.y - fragCoord.y);

    float offsetX = 26.77311 * iTime;
    float offsetY = 0.0;

    vec4 tex1 = lookup(coord.x + offsetX, coord.y + offsetY, 0.0);
    vec4 tex2 = lookup(coord.x * 2.0 + offsetX * 1.6, coord.y * 2.0 + offsetY * 1.5, 5.3);
    vec4 tex3 = lookup(coord.x * 3.0 + offsetX * 1.3, coord.y * 3.0 + offsetY * 1.2, 11.7);
    vec4 tex4 = lookup(coord.x * 0.5 + offsetX * 0.8, coord.y * 0.5 + offsetY * 0.9, 17.1);

    vec4 rays1 = vec4(1.0) * rayStrength(vec2(iResolution.x * 0.7, iResolution.y * -0.4), normalize(vec2(1.0, -0.116)), coord, 36.2214, 21.11349, 1.5);
    vec4 rays2 = vec4(1.0) * rayStrength(vec2(iResolution.x * 0.8, iResolution.y * -0.6), normalize(vec2(1.0, 0.241)), coord, 22.39910, 18.0234, 1.1);

    float bs = iResolution.x / 600.0;
    vec4 bubbles = vec4(1.0) * (
        bubbleStrength(vec2(0.0, 0.0),     vec2(offsetX * 0.2312, 0.0),                20.0 * bs, 60.0, coord) * 0.25 +
        bubbleStrength(vec2(40.0, 400.0),  vec2(offsetX * -0.06871, offsetY * 0.301),   7.0 * bs, 25.0, coord) * 0.10 +
        bubbleStrength(vec2(300.0, 70.0),  vec2(offsetX * 0.19832, offsetY * 0.1351),  14.0 * bs, 45.0, coord) * 0.18 +
        bubbleStrength(vec2(500.0, 280.0), vec2(offsetX * -0.0993, offsetY * -0.2654), 12.0 * bs, 32.0, coord) * 0.13);

    vec4 col = tex1 * 0.45 + tex2 * 0.25 + tex3 * 0.15 + tex4 * 0.1 + rays1 * 0.5 + rays2 * 0.4 + bubbles;

    float brightness = 1.0 - (coord.y / iResolution.y);
    col.x *= 0.2 + (brightness * 0.8);
    col.y *= 0.3 + (brightness * 0.7);
    col.z *= 0.4 + (brightness * 0.6);

    outColor = vec4(col.rgb, 1.0);
}
