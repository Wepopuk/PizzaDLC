#version 330
uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    fragColor = texture(Sampler0, texCoord) * vertexColor;
    if (fragColor.a == 0.0) discard;
}
