#version 330

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (vertexColor.a <= 0.001) discard;
    fragColor = vertexColor;
}
