#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 o = (1.0 / vec2(textureSize(InSampler, 0))) * 2.0;

    vec4 sum = texture(InSampler, texCoord) * 4.0;
    sum += texture(InSampler, texCoord + vec2( o.x, 0.0)) * 2.0;
    sum += texture(InSampler, texCoord + vec2(-o.x, 0.0)) * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0,  o.y)) * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, -o.y)) * 2.0;
    sum += texture(InSampler, texCoord + vec2( o.x,  o.y));
    sum += texture(InSampler, texCoord + vec2(-o.x,  o.y));
    sum += texture(InSampler, texCoord + vec2( o.x, -o.y));
    sum += texture(InSampler, texCoord + vec2(-o.x, -o.y));

    fragColor = sum / 16.0;
}
