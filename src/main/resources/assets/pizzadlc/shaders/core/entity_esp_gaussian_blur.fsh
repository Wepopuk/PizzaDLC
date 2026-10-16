#version 330

layout(std140) uniform EspBlur {
    vec2 TexelSize;
    vec2 Direction;
    vec4 Kernel;
    vec4 Weights;
};

uniform sampler2D InSampler;

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    vec4 sum = texture(InSampler, TexCoord) * Kernel.y;
    vec2 sampleStep = Direction * TexelSize * Kernel.x;
    for (int index = 1; index <= 5; index++) {
        float weight = index <= 2 ? Kernel[index + 1] : Weights[index - 3];
        vec2 offset = sampleStep * float(index);
        sum += texture(InSampler, TexCoord + offset) * weight;
        sum += texture(InSampler, TexCoord - offset) * weight;
    }
    fragColor = sum;
}
