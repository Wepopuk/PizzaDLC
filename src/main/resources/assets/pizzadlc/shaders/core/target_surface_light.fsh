#version 330

layout(std140) uniform TargetLighting {
    mat4 Projection;
    mat4 InverseProjection;
    vec4 Settings;
    vec4 Lights[4];
    vec4 Colors[4];
    mat4 InverseView;
    vec4 GridOrigin;
    uvec4 Occluders[256];
};

uniform sampler2D DepthSampler;
uniform sampler2D SceneSampler;
in vec2 TexCoord;
out vec4 fragColor;

vec3 positionAt(vec2 uv) {
    float depth = texture(DepthSampler, uv).r;
    vec4 position = InverseProjection * vec4(uv * 2.0 - 1.0, DIMASIK_DEPTH_TO_CLIP(depth), 1.0);
    return position.xyz / max(position.w, 0.000001);
}

float visibility(vec3 surface, vec3 normal, vec3 light) {
    vec3 start = ((InverseView * vec4(surface + normal * 0.04, 1.0)).xyz - GridOrigin.xyz) * GridOrigin.w;
    vec3 end = ((InverseView * vec4(light, 1.0)).xyz - GridOrigin.xyz) * GridOrigin.w;
    vec3 ray = end - start;
    ivec3 cell = ivec3(floor(start));
    ivec3 direction = ivec3(sign(ray));
    vec3 stride = 1.0 / max(abs(ray), vec3(0.000001));
    vec3 boundary = vec3(cell) + step(vec3(0.0), ray);
    vec3 next = abs(boundary - start) * stride;
    for (int index = 0; index < 24; index++) {
        if (any(lessThan(cell, ivec3(0))) || any(greaterThanEqual(cell, ivec3(32)))) return 1.0;
        int bit = cell.x + cell.y * 32 + cell.z * 1024;
        uint word = Occluders[bit >> 7][(bit >> 5) & 3];
        if ((word & (1u << uint(bit & 31))) != 0u) return 0.0;
        float distance = min(next.x, min(next.y, next.z));
        if (distance >= 1.0) return 1.0;
        vec3 advance = vec3(lessThanEqual(next, vec3(distance + 0.000001)));
        cell += ivec3(advance) * direction;
        next += advance * stride;
    }
    return 1.0;
}

void main() {
    if (texture(DepthSampler, TexCoord).r <= 0.000001) discard;
    vec3 position = positionAt(TexCoord);
    bool inRange = false;
    for (int index = 0; index < int(Settings.z); index++) {
        vec3 delta = Lights[index].xyz - position;
        inRange = inRange || dot(delta, delta) < Lights[index].w * Lights[index].w;
    }
    if (!inRange) discard;
    vec3 left = position - positionAt(TexCoord - vec2(Settings.x, 0.0));
    vec3 right = positionAt(TexCoord + vec2(Settings.x, 0.0)) - position;
    vec3 down = position - positionAt(TexCoord - vec2(0.0, Settings.y));
    vec3 up = positionAt(TexCoord + vec2(0.0, Settings.y)) - position;
    vec3 dx = abs(left.z) < abs(right.z) ? left : right;
    vec3 dy = abs(down.z) < abs(up.z) ? down : up;
    float dxLength = dot(dx, dx);
    float dyLength = dot(dy, dy);
    if (dxLength <= 0.000000000001 || dyLength <= 0.000000000001) discard;
    vec3 normal = cross(dx * inversesqrt(dxLength), dy * inversesqrt(dyLength));
    float normalLength = dot(normal, normal);
    if (normalLength <= 0.000001) discard;
    normal *= inversesqrt(normalLength);
    if (dot(normal, -position) < 0.0) normal = -normal;
    vec3 illumination = vec3(0.0);
    for (int index = 0; index < int(Settings.z); index++) {
        vec3 delta = Lights[index].xyz - position;
        float distanceSquared = dot(delta, delta);
        float rangeSquared = Lights[index].w * Lights[index].w;
        if (distanceSquared >= rangeSquared) continue;
        float diffuse = max(dot(normal, delta * inversesqrt(max(distanceSquared, 0.0001))), 0.0);
        if (diffuse <= 0.001) continue;
        float falloff = 1.0 - distanceSquared / rangeSquared;
        float attenuation = falloff * falloff / (1.0 + distanceSquared * 0.6);
        float energy = diffuse * attenuation * Colors[index].a;
        if (energy <= 0.001) continue;
        illumination += Colors[index].rgb * energy * visibility(position, normal, Lights[index].xyz);
    }
    illumination = min(illumination * Settings.w, vec3(1.8));
    if (max(illumination.r, max(illumination.g, illumination.b)) <= 0.001) discard;
    fragColor = vec4(texture(SceneSampler, TexCoord).rgb * illumination, 0.0);
}
