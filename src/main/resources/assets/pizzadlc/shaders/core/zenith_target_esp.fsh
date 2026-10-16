#version 330

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const float FUSED_ADDITIVE_SCALE = 2.0;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
     float coverage = texel.a * vertexColor.a;
    if (coverage == 0.0) {
        discard;
    }
    vec3 tint = texel.rgb * vertexColor.rgb;
    fragColor = vec4(tint * coverage * FUSED_ADDITIVE_SCALE, coverage);
}
