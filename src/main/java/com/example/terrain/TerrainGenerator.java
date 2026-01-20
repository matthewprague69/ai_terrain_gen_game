package com.example.terrain;

public class TerrainGenerator {
    private final Noise2D noise;
    private final int octaves;
    private final float lacunarity;
    private final float gain;
    private final BiomeMap biomeMap;

    public TerrainGenerator(int seed) {
        this.noise = new Noise2D(seed);
        this.octaves = 5;
        this.lacunarity = 2.0f;
        this.gain = 0.5f;
        this.biomeMap = new BiomeMap(seed ^ 0x5f3759df, 256.0f);
    }

    public float getHeight(float worldX, float worldZ) {
        BiomeSample sample = biomeMap.sample(worldX, worldZ);
        float height = 0.0f;
        height += sample.grasslandWeight() * evalBiome(BiomeType.GRASSLAND, worldX, worldZ);
        height += sample.highlandsWeight() * evalBiome(BiomeType.HIGHLANDS, worldX, worldZ);
        height += sample.tundraWeight() * evalBiome(BiomeType.TUNDRA, worldX, worldZ);
        return height;
    }

    public BiomeSample sampleBiome(float worldX, float worldZ) {
        return biomeMap.sample(worldX, worldZ);
    }

    private float evalBiome(BiomeType biome, float worldX, float worldZ) {
        float height = noise.fbm(worldX * biome.getFrequency(), worldZ * biome.getFrequency(), octaves, lacunarity, gain);
        return biome.getBaseHeight() + height * biome.getAmplitude();
    }
}
