#version 330

uniform sampler2D InSampler;

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(InSampler, TexCoord);
    if (color.a <= 0.001) discard;
    fragColor = color;
}
