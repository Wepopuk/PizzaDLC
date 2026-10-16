#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 g = texture(Sampler0, texCoord);
    float inside = texture(Sampler1, texCoord).a;
    float a = g.a * ((1.0 - inside) * 8.0 + inside);
    fragColor = vec4(g.rgb, min(a, 1.0));
}
