package com.example.terrain;

import org.joml.Vector3f;

public class ChunkMeshBuilder {
    private final TerrainGenerator generator;
    private final int chunkSize;

    public ChunkMeshBuilder(TerrainGenerator generator, int chunkSize) {
        this.generator = generator;
        this.chunkSize = chunkSize;
    }

    public ChunkMeshData build(int chunkX, int chunkZ, int lod) {
        int step = lod == 0 ? 1 : 2;
        int vertsPerSide = (chunkSize / step) + 1;
        int vertexCount = vertsPerSide * vertsPerSide;
        float[] vertices = new float[vertexCount * 11];

        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        int index = 0;
        float uvScale = 0.1f;
        for (int z = 0; z < vertsPerSide; z++) {
            for (int x = 0; x < vertsPerSide; x++) {
                float worldX = chunkX * chunkSize + x * step;
                float worldZ = chunkZ * chunkSize + z * step;
                float height = generator.getHeight(worldX, worldZ);
                BiomeSample biomeSample = generator.sampleBiome(worldX, worldZ);
                minY = Math.min(minY, height);
                maxY = Math.max(maxY, height);

                vertices[index++] = worldX;
                vertices[index++] = height;
                vertices[index++] = worldZ;
                vertices[index++] = 0.0f;
                vertices[index++] = 1.0f;
                vertices[index++] = 0.0f;
                vertices[index++] = worldX * uvScale;
                vertices[index++] = worldZ * uvScale;
                vertices[index++] = biomeSample.grasslandWeight();
                vertices[index++] = biomeSample.highlandsWeight();
                vertices[index++] = biomeSample.tundraWeight();
            }
        }

        int quadCount = (vertsPerSide - 1) * (vertsPerSide - 1);
        int[] indices = new int[quadCount * 6];
        int idx = 0;
        for (int z = 0; z < vertsPerSide - 1; z++) {
            for (int x = 0; x < vertsPerSide - 1; x++) {
                int topLeft = z * vertsPerSide + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * vertsPerSide + x;
                int bottomRight = bottomLeft + 1;

                indices[idx++] = topLeft;
                indices[idx++] = bottomLeft;
                indices[idx++] = topRight;

                indices[idx++] = topRight;
                indices[idx++] = bottomLeft;
                indices[idx++] = bottomRight;
            }
        }

        recalcNormals(vertices, indices);
        return new ChunkMeshData(vertices, indices, minY, maxY, lod);
    }

    private void recalcNormals(float[] vertices, int[] indices) {
        Vector3f v0 = new Vector3f();
        Vector3f v1 = new Vector3f();
        Vector3f v2 = new Vector3f();
        Vector3f edge1 = new Vector3f();
        Vector3f edge2 = new Vector3f();
        Vector3f normal = new Vector3f();

        for (int i = 0; i < indices.length; i += 3) {
            int i0 = indices[i] * 11;
            int i1 = indices[i + 1] * 11;
            int i2 = indices[i + 2] * 11;

            v0.set(vertices[i0], vertices[i0 + 1], vertices[i0 + 2]);
            v1.set(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]);
            v2.set(vertices[i2], vertices[i2 + 1], vertices[i2 + 2]);

            edge1.set(v1).sub(v0);
            edge2.set(v2).sub(v0);
            normal.set(edge1).cross(edge2).normalize();

            accumulateNormal(vertices, i0, normal);
            accumulateNormal(vertices, i1, normal);
            accumulateNormal(vertices, i2, normal);
        }

        for (int i = 0; i < vertices.length; i += 11) {
            Vector3f n = new Vector3f(vertices[i + 3], vertices[i + 4], vertices[i + 5]).normalize();
            vertices[i + 3] = n.x;
            vertices[i + 4] = n.y;
            vertices[i + 5] = n.z;
        }
    }

    private void accumulateNormal(float[] vertices, int index, Vector3f normal) {
        vertices[index + 3] += normal.x;
        vertices[index + 4] += normal.y;
        vertices[index + 5] += normal.z;
    }
}
