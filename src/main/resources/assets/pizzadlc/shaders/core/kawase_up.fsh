#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texel = 1.0 / vec2(textureSize(InSampler, 0));
    vec2 o = texel * 2.0;

    vec3 sum = vec3(0.0);
    sum += texture(InSampler, texCoord + vec2(-o.x * 2.0, 0.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(-o.x,  o.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2( 0.0,  o.y * 2.0)).rgb;
    sum += texture(InSampler, texCoord + vec2( o.x,  o.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2( o.x * 2.0, 0.0)).rgb;
    sum += texture(InSampler, texCoord + vec2( o.x, -o.y)).rgb * 2.0;
    sum += texture(InSampler, texCoord + vec2( 0.0, -o.y * 2.0)).rgb;
    sum += texture(InSampler, texCoord + vec2(-o.x, -o.y)).rgb * 2.0;

    fragColor = vec4(sum / 12.0, 1.0);
}
