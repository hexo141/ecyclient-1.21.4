#version 150 core

uniform sampler2D uPreviousFrame;
uniform float uStrength;

in vec2 vTexCoord;
out vec4 fragColor;

void main() {
    vec4 current = texture(uPreviousFrame, vTexCoord);
    fragColor = current;
}
