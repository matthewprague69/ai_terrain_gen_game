package com.example.terrain;

import org.joml.Vector3f;

public enum BiomeType {
    GRASSLAND(6.0f, 14.0f, 0.008f, new Vector3f(1.2f, 0.6f, 0.2f)),
    HIGHLANDS(12.0f, 28.0f, 0.012f, new Vector3f(0.6f, 1.2f, 0.6f)),
    TUNDRA(10.0f, 18.0f, 0.01f, new Vector3f(0.4f, 0.7f, 1.4f));

    private final float baseHeight;
    private final float amplitude;
    private final float frequency;
    private final Vector3f textureWeights;

    BiomeType(float baseHeight, float amplitude, float frequency, Vector3f textureWeights) {
        this.baseHeight = baseHeight;
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.textureWeights = textureWeights;
    }

    public float getBaseHeight() {
        return baseHeight;
    }

    public float getAmplitude() {
        return amplitude;
    }

    public float getFrequency() {
        return frequency;
    }

    public Vector3f getTextureWeights() {
        return new Vector3f(textureWeights);
    }
}
