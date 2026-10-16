#version 330

const float PI = 3.14159265359;
const float TAU = 6.28318530718;
const float RAD = 0.0174533;
const float START = -1.5707963;

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec4 vertexColor;
in vec4 colorB;
in vec2 localCoord;
in float radiusN;
in float thickN;
in float endDeg;
in float phase;

out vec4 fragColor;

void main() {
    vec2 centerPos = localCoord * 2.0 - 1.0;
    centerPos.y = -centerPos.y;
    float dist = length(centerPos);

    float ringMid = radiusN + thickN * 0.5;
    float dRad = abs(dist - ringMid) - thickN * 0.5;
    float bandAlpha = clamp(0.5 - dRad / max(fwidth(dRad), 1e-5), 0.0, 1.0);

    float sweepRad = min(endDeg * RAD, TAU);
    float ang = atan(centerPos.y, centerPos.x) + PI;
    float rel = mod(ang - START, TAU);
    float angleAlpha;
    if (sweepRad >= TAU - 0.001) {
        angleAlpha = 1.0;
    } else {
        float relS = (rel > sweepRad + (TAU - sweepRad) * 0.5) ? (rel - TAU) : rel;
        float aaA = max(fwidth(dist), 1e-5) / max(dist, 0.05);
        float aStart = clamp((relS + aaA) / (2.0 * aaA), 0.0, 1.0);
        float aEnd = clamp((sweepRad - relS + aaA) / (2.0 * aaA), 0.0, 1.0);
        angleAlpha = aStart * aEnd;
    }

    float g = 0.5 - 0.5 * cos(ang - START + phase * TAU);
    vec4 col = mix(vertexColor, colorB, g) * ColorModulator;
    float a = col.a * bandAlpha * angleAlpha;
    if (a <= 0.0) discard;
    fragColor = vec4(col.rgb, a);
}
