#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 t = 1.0 / vec2(textureSize(InSampler, 0));

    vec4 c = texture(InSampler, texCoord);
    vec4 l = texture(InSampler, texCoord - vec2(t.x, 0.0));
    vec4 r = texture(InSampler, texCoord + vec2(t.x, 0.0));
    vec4 u = texture(InSampler, texCoord - vec2(0.0, t.y));
    vec4 d = texture(InSampler, texCoord + vec2(0.0, t.y));

    float edge = clamp(abs(c.a - l.a) + abs(c.a - r.a) + abs(c.a - u.a) + abs(c.a - d.a), 0.0, 1.0);
    vec3 col = c.rgb * c.a + l.rgb * l.a + r.rgb * r.a + u.rgb * u.a + d.rgb * d.a;

    fragColor = vec4(col * 0.25, clamp(edge * 2.5, 0.0, 1.0));
}
