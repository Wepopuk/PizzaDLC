#version 330

const float RANGE = 12.0;
const float THICKNESS = 0.0;
const float SMOOTHNESS = 0.5;

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float median(vec3 c) {
    return max(min(c.r, c.g), min(max(c.r, c.g), c.b));
}

void main() {
    float dist = median(texture(Sampler0, texCoord0).rgb) - 0.5 + THICKNESS;
    vec2 h = vec2(dFdx(texCoord0.x), dFdy(texCoord0.y)) * vec2(textureSize(Sampler0, 0));
    float pixels = RANGE * inversesqrt(h.x * h.x + h.y * h.y);
    float alpha = smoothstep(-SMOOTHNESS, SMOOTHNESS, dist * pixels);
    vec4 c = vertexColor * ColorModulator;
    c.a *= alpha;
    if (c.a <= 0.0) discard;
    fragColor = c;
}
