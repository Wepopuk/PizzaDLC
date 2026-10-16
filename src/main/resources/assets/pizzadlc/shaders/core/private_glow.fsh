#version 330

uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec4 sampled = texture(Sampler0, texCoord);
    float alpha = sampled.a * vertexColor.a;
    if (alpha <= 0.001) discard;
    fragColor = vec4(sampled.rgb * vertexColor.rgb, alpha);
}
