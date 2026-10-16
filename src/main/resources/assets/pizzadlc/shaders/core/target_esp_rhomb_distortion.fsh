#version 330

uniform sampler2D SceneSampler;
uniform sampler2D GlowSampler;
layout(std140) uniform GlowDistortion {
    float Strength;
};

in vec2 TexCoord;
out vec4 fragColor;

float glowAt(vec2 uv) {
    vec4 glow = texture(GlowSampler, clamp(uv, vec2(0.0), vec2(1.0)));
    return glow.a;
}

void main() {
    vec2 texel = 1.0 / vec2(textureSize(GlowSampler, 0));
    float mask = clamp(glowAt(TexCoord), 0.0, 1.0);
    if (mask <= 0.0 || mask >= 1.0) discard;

    float left = glowAt(TexCoord - vec2(texel.x * 4.0, 0.0));
    float right = glowAt(TexCoord + vec2(texel.x * 4.0, 0.0));
    float down = glowAt(TexCoord - vec2(0.0, texel.y * 4.0));
    float up = glowAt(TexCoord + vec2(0.0, texel.y * 4.0));
    vec2 gradient = vec2(right - left, up - down);

    float halo = sin(clamp(mask, 0.0, 1.0) * 3.14159265);
    float gradientLengthSquared = dot(gradient, gradient);
    if (gradientLengthSquared <= 0.00000001) discard;
    vec2 direction = gradient * inversesqrt(gradientLengthSquared);
    vec2 offset = direction * texel * Strength * halo;

    vec2 uv = clamp(TexCoord + offset, texel, vec2(1.0) - texel);
    vec3 scene;
    scene.r = texture(SceneSampler, clamp(uv + offset * 0.22, texel, vec2(1.0) - texel)).r;
    scene.g = texture(SceneSampler, uv).g;
    scene.b = texture(SceneSampler, clamp(uv - offset * 0.22, texel, vec2(1.0) - texel)).b;

    fragColor = vec4(scene, texture(SceneSampler, TexCoord).a);
}
