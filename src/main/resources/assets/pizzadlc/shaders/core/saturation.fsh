#version 330

uniform sampler2D InSampler;

layout(std140) uniform SatConfig {
    float uSaturation;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 c = texture(InSampler, texCoord);
    float luma = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
    fragColor = vec4(mix(vec3(luma), c.rgb, uSaturation), c.a);
}
