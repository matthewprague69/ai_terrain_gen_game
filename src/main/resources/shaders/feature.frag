#version 460 core

in vec3 vNormal;
in vec3 vWorldPos;
in vec3 vColor;
in vec3 vViewPos;

out vec4 fragColor;

uniform vec3 uLightDir;
uniform vec3 uCameraPos;
uniform vec3 uFogColor;
uniform float uFogStart;
uniform float uFogEnd;
uniform float uShadowBias;
uniform sampler2DArrayShadow uShadowMap;
uniform mat4 uLightViewProj[3];
uniform float uCascadeSplits[3];

float sampleShadow(int cascadeIndex, vec4 lightSpacePosition) {
    vec3 projCoords = lightSpacePosition.xyz / lightSpacePosition.w;
    projCoords = projCoords * 0.5 + 0.5;
    if (projCoords.z > 1.0) {
        return 1.0;
    }
    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(uShadowMap, 0).xy);
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(x, y) * texelSize;
            shadow += texture(uShadowMap, vec4(projCoords.xy + offset, cascadeIndex, projCoords.z - uShadowBias));
        }
    }
    return shadow / 9.0;
}

int selectCascade(float viewDepth) {
    for (int i = 0; i < 3; i++) {
        if (viewDepth < uCascadeSplits[i]) {
            return i;
        }
    }
    return 2;
}

void main() {
    vec3 normal = normalize(vNormal);
    vec3 lightDir = normalize(-uLightDir);
    float diffuse = max(dot(normal, lightDir), 0.2);

    float viewDepth = -vViewPos.z;
    int cascadeIndex = selectCascade(viewDepth);
    vec4 lightSpacePosition = uLightViewProj[cascadeIndex] * vec4(vWorldPos, 1.0);
    float shadowFactor = sampleShadow(cascadeIndex, lightSpacePosition);

    vec3 litColor = vColor * diffuse * shadowFactor;

    float distanceToCamera = length(uCameraPos - vWorldPos);
    float fogFactor = clamp((uFogEnd - distanceToCamera) / (uFogEnd - uFogStart), 0.0, 1.0);
    vec3 finalColor = mix(uFogColor, litColor, fogFactor);

    fragColor = vec4(finalColor, 1.0);
}
