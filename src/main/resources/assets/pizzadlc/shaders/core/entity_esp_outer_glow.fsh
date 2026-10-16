#version 330

uniform sampler2D BloomSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform EspOuterGlow {
    vec4 Params;
    vec4 GlowColor1;
    vec4 GlowColor2;
    vec4 OutlineColor;
    vec4 Screen;
};

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    vec4 bloom = texture(BloomSampler, TexCoord);
    float mask = texture(MaskSampler, TexCoord).a;
    float entityMask = smoothstep(0.01, 0.12, mask);
    float bloomMask = clamp(bloom.a, 0.0, 1.0);
    float outerGlow = pow(bloomMask, 0.72) * (1.0 - entityMask);
    float innerDistance = clamp((1.0 - bloomMask) * 2.15, 0.0, 1.0);
    float innerSupport = smoothstep(0.04, 0.32, bloomMask);
    float cornerSupport = smoothstep(0.015, 0.18, bloomMask) * 0.42;
    innerSupport = max(innerSupport, cornerSupport);
    float innerGlow = entityMask * pow(innerDistance, 0.62) * innerSupport * Params.w;
    float alphaMask = max(outerGlow, innerGlow * 0.82);
    vec2 outlineOffset = Screen.z / max(Screen.xy, vec2(1.0));
    float outlineMask = 0.0;
    if (OutlineColor.a > 0.001 && Screen.z > 0.0) {
        outlineMask = max(outlineMask, texture(MaskSampler, TexCoord + vec2(outlineOffset.x, 0.0)).a);
        outlineMask = max(outlineMask, texture(MaskSampler, TexCoord - vec2(outlineOffset.x, 0.0)).a);
        outlineMask = max(outlineMask, texture(MaskSampler, TexCoord + vec2(0.0, outlineOffset.y)).a);
        outlineMask = max(outlineMask, texture(MaskSampler, TexCoord - vec2(0.0, outlineOffset.y)).a);
        outlineMask = max(outlineMask, texture(MaskSampler, TexCoord + outlineOffset * 0.7071).a);
        outlineMask = max(outlineMask, texture(MaskSampler, TexCoord + vec2(-outlineOffset.x, outlineOffset.y) * 0.7071).a);
        outlineMask = max(outlineMask, texture(MaskSampler, TexCoord + vec2(outlineOffset.x, -outlineOffset.y) * 0.7071).a);
        outlineMask = max(outlineMask, texture(MaskSampler, TexCoord - outlineOffset * 0.7071).a);
    }
    float outlineGlow = smoothstep(0.02, 0.8, outlineMask)
        * (1.0 - entityMask) * OutlineColor.a;
    if (max(alphaMask, outlineGlow) < 0.002) discard;

    float outerIntensity = outerGlow * Params.x;
    float innerIntensity = innerGlow * Params.x * 0.78;
    float intensity = outerIntensity + innerIntensity * (1.0 - clamp(outerIntensity, 0.0, 1.0));
    float hot = smoothstep(0.65, 1.55, intensity);
    vec3 base;
    if (Params.y > 1.5) {
        vec3 sampled = bloom.rgb / max(bloom.a, 0.0001);
        float core = pow(clamp(alphaMask, 0.0, 1.0), 0.55);
        base = mix(sampled * 0.5, sampled, core);
    } else if (Params.y > 0.5) {
        float confidence = smoothstep(0.03, 0.25, bloom.a);
        vec3 color = bloom.rgb / max(bloom.a, 0.08);
        float maximum = max(color.r, max(color.g, color.b));
        if (maximum > 0.001) color /= maximum;
        float luminance = dot(color, vec3(0.299, 0.587, 0.114));
        vec3 saturated = clamp(mix(vec3(luminance), color, Params.z), 0.0, 1.0);
        base = mix(vec3(1.0), saturated, confidence);
    } else {
        float core = pow(clamp(alphaMask, 0.0, 1.0), 0.55);
        base = mix(GlowColor2.rgb, GlowColor1.rgb, core);
    }
    vec3 color = mix(base, vec3(1.0, 0.97, 0.88), hot * 0.85);
    float innerCore = smoothstep(0.08, 0.72, innerGlow);
    color = mix(color, mix(base, vec3(1.0), 0.72), innerCore * 0.58);
    vec3 outlineColor = Params.y > 0.5 ? base : OutlineColor.rgb;
    color = mix(color, outlineColor, clamp(outlineGlow * 1.35, 0.0, 1.0));
    float brightness = 1.0 + hot * 0.6 + innerCore * 0.34;
    fragColor = vec4(color * brightness, clamp(max(intensity, outlineGlow), 0.0, 1.0));
}
