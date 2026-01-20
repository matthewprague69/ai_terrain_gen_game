#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUv;
layout(location = 3) in vec3 aBiomeWeights;

out vec3 vNormal;
out vec3 vWorldPos;
out vec2 vUv;
out vec3 vViewPos;
out vec3 vBiomeWeights;

uniform mat4 uProjection;
uniform mat4 uView;

void main() {
    vec4 worldPos = vec4(aPosition, 1.0);
    vec4 viewPos = uView * worldPos;
    vNormal = aNormal;
    vWorldPos = aPosition;
    vViewPos = viewPos.xyz;
    vUv = aUv;
    vBiomeWeights = aBiomeWeights;
    gl_Position = uProjection * viewPos;
}
