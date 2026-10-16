#version 330

uniform sampler2D InSampler;

in vec2 TexCoord;

out vec4 fragColor;

void main() {
    float mask = texture(InSampler, TexCoord).a;
    float alpha = smoothstep(0.025, 0.28, mask);
    if (alpha <= 0.002) discard;
    fragColor = vec4(1.0, 1.0, 1.0, alpha);
}
