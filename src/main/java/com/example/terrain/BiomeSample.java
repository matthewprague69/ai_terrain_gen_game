package com.example.terrain;

public record BiomeSample(float grasslandWeight, float highlandsWeight, float tundraWeight) {
    public float[] asArray() {
        return new float[]{grasslandWeight, highlandsWeight, tundraWeight};
    }
}
