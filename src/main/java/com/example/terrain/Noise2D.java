package com.example.terrain;

public class Noise2D {
    private final int seed;

    public Noise2D(int seed) {
        this.seed = seed;
    }

    public float fbm(float x, float z, int octaves, float lacunarity, float gain) {
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float sum = 0.0f;
        float max = 0.0f;
        for (int i = 0; i < octaves; i++) {
            sum += amplitude * perlin(x * frequency, z * frequency);
            max += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }
        return sum / max;
    }

    private float perlin(float x, float z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        float sx = fade(x - x0);
        float sz = fade(z - z0);

        float n00 = gradDot(x0, z0, x - x0, z - z0);
        float n10 = gradDot(x1, z0, x - x1, z - z0);
        float n01 = gradDot(x0, z1, x - x0, z - z1);
        float n11 = gradDot(x1, z1, x - x1, z - z1);

        float ix0 = lerp(n00, n10, sx);
        float ix1 = lerp(n01, n11, sx);
        return lerp(ix0, ix1, sz);
    }

    private float gradDot(int gx, int gz, float dx, float dz) {
        int hash = hash(gx, gz);
        int h = hash & 3;
        float gradX = (h == 0 || h == 3) ? 1.0f : -1.0f;
        float gradZ = (h == 0 || h == 1) ? 1.0f : -1.0f;
        return gradX * dx + gradZ * dz;
    }

    private int hash(int x, int z) {
        int h = seed;
        h ^= x * 0x27d4eb2d;
        h ^= z * 0x165667b1;
        h = Integer.rotateLeft(h, 13);
        h *= 0x85ebca6b;
        h ^= h >>> 16;
        return h;
    }

    private int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }
}
