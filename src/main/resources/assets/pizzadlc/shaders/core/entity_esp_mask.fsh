#version 330

uniform sampler2D InSampler;

in vec2 TexCoord;
out vec4 fragColor;

int proceduralCategory(vec4 color) {
    ivec3 bytes = ivec3(round(color.rgb / max(color.a, 0.01) * 255.0));
    if (bytes.r >= 1 && bytes.r <= 6 && bytes.g >= 253 && bytes.b >= 252) {
        return bytes.r;
    }
    return 0;
}

vec3 categoryColor(int category) {
    if (category == 1) return vec3(1.0);
    if (category == 2) return vec3(1.0, 0.31, 0.31);
    if (category == 3) return vec3(0.35, 0.90, 0.51);
    if (category == 4) return vec3(0.35, 0.82, 0.90);
    if (category == 5) return vec3(1.0, 0.80, 0.31);
    if (category == 6) return vec3(0.35, 0.67, 1.0);
    return vec3(1.0);
}

void main() {
    vec4 color = texture(InSampler, TexCoord);
    vec3 outputColor = color.rgb;
    int category = proceduralCategory(color);
    if (category > 0) {
        outputColor = categoryColor(category);
    }
    fragColor = vec4(outputColor * color.a, color.a);
}
