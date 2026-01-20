package com.example.terrain;

import org.joml.Vector2f;

public class BiomeMap {
    private final int seed;
    private final float biomeSize;

    public BiomeMap(int seed, float biomeSize) {
        this.seed = seed;
        this.biomeSize = biomeSize;
    }

    public BiomeSample sample(float worldX, float worldZ) {
        float cellX = worldX / biomeSize;
        float cellZ = worldZ / biomeSize;
        int baseX = (int) Math.floor(cellX);
        int baseZ = (int) Math.floor(cellZ);

        float closestDist = Float.POSITIVE_INFINITY;
        float secondDist = Float.POSITIVE_INFINITY;
        BiomeType closestBiome = BiomeType.GRASSLAND;
        BiomeType secondBiome = BiomeType.HIGHLANDS;

        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int cellOffsetX = baseX + dx;
                int cellOffsetZ = baseZ + dz;
                Vector2f offset = randomOffset(cellOffsetX, cellOffsetZ);
                float featureX = (cellOffsetX + offset.x) * biomeSize;
                float featureZ = (cellOffsetZ + offset.y) * biomeSize;
                float dist = distanceSquared(worldX, worldZ, featureX, featureZ);
                BiomeType biome = pickBiome(cellOffsetX, cellOffsetZ);

                if (dist < closestDist) {
                    secondDist = closestDist;
                    secondBiome = closestBiome;
                    closestDist = dist;
                    closestBiome = biome;
                } else if (dist < secondDist) {
                    secondDist = dist;
                    secondBiome = biome;
                }
            }
        }

        float blend = smoothStep(0.0f, 1.0f, (float) Math.sqrt(closestDist / (secondDist + 0.0001f)));
        float primaryWeight = 1.0f - blend;
        float secondaryWeight = blend;

        float grass = 0.0f;
        float high = 0.0f;
        float tundra = 0.0f;

        grass += closestBiome == BiomeType.GRASSLAND ? primaryWeight : 0.0f;
        high += closestBiome == BiomeType.HIGHLANDS ? primaryWeight : 0.0f;
        tundra += closestBiome == BiomeType.TUNDRA ? primaryWeight : 0.0f;

        grass += secondBiome == BiomeType.GRASSLAND ? secondaryWeight : 0.0f;
        high += secondBiome == BiomeType.HIGHLANDS ? secondaryWeight : 0.0f;
        tundra += secondBiome == BiomeType.TUNDRA ? secondaryWeight : 0.0f;

        float sum = grass + high + tundra + 0.0001f;
        return new BiomeSample(grass / sum, high / sum, tundra / sum);
    }

    private Vector2f randomOffset(int cellX, int cellZ) {
        int hash = hash(cellX, cellZ);
        float x = ((hash & 0xffff) / 65535.0f);
        float z = (((hash >>> 16) & 0xffff) / 65535.0f);
        return new Vector2f(x, z);
    }

    private BiomeType pickBiome(int cellX, int cellZ) {
        int hash = hash(cellX * 73856093, cellZ * 19349663);
        int value = Math.floorMod(hash, 100);
        if (value < 45) {
            return BiomeType.GRASSLAND;
        }
        if (value < 75) {
            return BiomeType.HIGHLANDS;
        }
        return BiomeType.TUNDRA;
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

    private float distanceSquared(float x0, float z0, float x1, float z1) {
        float dx = x0 - x1;
        float dz = z0 - z1;
        return dx * dx + dz * dz;
    }

    private float smoothStep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
