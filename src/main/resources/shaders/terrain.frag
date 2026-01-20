#version 460 core

in vec3 vNormal;
in vec3 vWorldPos;
in vec2 vUv;
in vec3 vViewPos;
in vec3 vBiomeWeights;

out vec4 fragColor;

uniform sampler2DArray uTerrainTextures;
uniform sampler2DArrayShadow uShadowMap;

uniform vec3 uLightDir;
uniform vec3 uBaseColor;
uniform vec3 uCameraPos;
uniform vec3 uFogColor;
uniform float uFogStart;
uniform float uFogEnd;
uniform float uShadowBias;
uniform mat4 uLightViewProj[3];
uniform float uCascadeSplits[3];
uniform vec3 uBiomeTextureWeights[3];

vec3 sampleLayer(int layer, vec2 uv) {
    return texture(uTerrainTextures, vec3(uv, layer)).rgb;
}

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

    float slope = 1.0 - clamp(normal.y, 0.0, 1.0);
    float height = vWorldPos.y;

    float grassWeight = clamp(1.0 - slope * 3.0, 0.0, 1.0) * clamp(1.0 - (height - 10.0) / 20.0, 0.0, 1.0);
    float rockWeight = clamp(slope * 3.0, 0.0, 1.0);
    float snowWeight = clamp((height - 28.0) / 16.0, 0.0, 1.0);

    vec3 biomeWeights = clamp(vBiomeWeights, 0.0, 1.0);
    vec3 biomeBlend = uBiomeTextureWeights[0] * biomeWeights.x
            + uBiomeTextureWeights[1] * biomeWeights.y
            + uBiomeTextureWeights[2] * biomeWeights.z;

    vec3 materialWeights = vec3(grassWeight, rockWeight, snowWeight) * biomeBlend;
    float weightSum = materialWeights.x + materialWeights.y + materialWeights.z + 0.0001;
    materialWeights /= weightSum;

    vec3 grass = sampleLayer(0, vUv * 0.7);
    vec3 rock = sampleLayer(1, vUv * 1.1);
    vec3 snow = sampleLayer(2, vUv * 0.9);

    vec3 albedo = grass * materialWeights.x + rock * materialWeights.y + snow * materialWeights.z;
    albedo = mix(albedo, uBaseColor, 0.15);

    float diffuse = max(dot(normal, lightDir), 0.2);

    float viewDepth = -vViewPos.z;
    int cascadeIndex = selectCascade(viewDepth);
    vec4 lightSpacePosition = uLightViewProj[cascadeIndex] * vec4(vWorldPos, 1.0);
    float shadowFactor = sampleShadow(cascadeIndex, lightSpacePosition);

    vec3 litColor = albedo * diffuse * shadowFactor;

    float distanceToCamera = length(uCameraPos - vWorldPos);
    float fogFactor = clamp((uFogEnd - distanceToCamera) / (uFogEnd - uFogStart), 0.0, 1.0);
    vec3 finalColor = mix(uFogColor, litColor, fogFactor);

    fragColor = vec4(finalColor, 1.0);
}
