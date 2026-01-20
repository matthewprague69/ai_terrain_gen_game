#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec3 aColor;

out vec3 vNormal;
out vec3 vWorldPos;
out vec3 vColor;
out vec3 vViewPos;

uniform mat4 uProjection;
uniform mat4 uView;

void main() {
    vec4 worldPos = vec4(aPosition, 1.0);
    vec4 viewPos = uView * worldPos;
    vNormal = aNormal;
    vWorldPos = aPosition;
    vViewPos = viewPos.xyz;
    vColor = aColor;
    gl_Position = uProjection * viewPos;
}
