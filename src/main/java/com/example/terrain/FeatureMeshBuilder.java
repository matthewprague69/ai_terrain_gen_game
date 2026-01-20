package com.example.terrain;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class FeatureMeshBuilder {
    private final TerrainGenerator generator;
    private final int chunkSize;
    private final int seed;

    public FeatureMeshBuilder(TerrainGenerator generator, int chunkSize, int seed) {
        this.generator = generator;
        this.chunkSize = chunkSize;
        this.seed = seed;
    }

    public FeatureMeshData buildNear(int chunkX, int chunkZ) {
        MeshDataBuilder builder = new MeshDataBuilder();
        spawnGrass(builder, chunkX, chunkZ);
        spawnRocks(builder, chunkX, chunkZ);
        spawnTrees(builder, chunkX, chunkZ, true);
        return builder.toMeshData();
    }

    public FeatureMeshData buildFar(int chunkX, int chunkZ) {
        MeshDataBuilder builder = new MeshDataBuilder();
        spawnTrees(builder, chunkX, chunkZ, false);
        return builder.toMeshData();
    }

    private void spawnGrass(MeshDataBuilder builder, int chunkX, int chunkZ) {
        int spacing = 4;
        float chance = 0.55f;
        for (int z = 0; z < chunkSize; z += spacing) {
            for (int x = 0; x < chunkSize; x += spacing) {
                int worldX = chunkX * chunkSize + x;
                int worldZ = chunkZ * chunkSize + z;
                if (random(worldX, worldZ, 101) > chance) {
                    continue;
                }
                float height = generator.getHeight(worldX, worldZ);
                Vector3f color = new Vector3f(0.25f, 0.6f, 0.2f);
                builder.addCrossQuad(worldX + 0.5f, height, worldZ + 0.5f, 0.2f, 0.8f, color);
            }
        }
    }

    private void spawnRocks(MeshDataBuilder builder, int chunkX, int chunkZ) {
        int spacing = 12;
        float chance = 0.25f;
        for (int z = 0; z < chunkSize; z += spacing) {
            for (int x = 0; x < chunkSize; x += spacing) {
                int worldX = chunkX * chunkSize + x;
                int worldZ = chunkZ * chunkSize + z;
                if (random(worldX, worldZ, 203) > chance) {
                    continue;
                }
                float height = generator.getHeight(worldX, worldZ);
                float scale = 0.4f + random(worldX, worldZ, 17) * 0.6f;
                Vector3f color = new Vector3f(0.45f, 0.45f, 0.45f);
                builder.addBox(worldX + 0.5f, height + scale * 0.5f, worldZ + 0.5f, scale, scale * 0.6f, scale, color);
            }
        }
    }

    private void spawnTrees(MeshDataBuilder builder, int chunkX, int chunkZ, boolean near) {
        int spacing = 20;
        float chance = 0.18f;
        for (int z = 0; z < chunkSize; z += spacing) {
            for (int x = 0; x < chunkSize; x += spacing) {
                int worldX = chunkX * chunkSize + x;
                int worldZ = chunkZ * chunkSize + z;
                if (random(worldX, worldZ, 401) > chance) {
                    continue;
                }
                float height = generator.getHeight(worldX, worldZ);
                float trunkHeight = 2.5f + random(worldX, worldZ, 99) * 1.5f;
                float trunkRadius = 0.25f;
                Vector3f trunkColor = new Vector3f(0.35f, 0.22f, 0.1f);
                if (near) {
                    builder.addBox(worldX + 0.5f, height + trunkHeight * 0.5f, worldZ + 0.5f, trunkRadius, trunkHeight, trunkRadius, trunkColor);
                    Vector3f leaves = new Vector3f(0.2f, 0.5f, 0.2f);
                    builder.addCrossQuad(worldX + 0.5f, height + trunkHeight, worldZ + 0.5f, 1.2f, 1.6f, leaves);
                } else {
                    Vector3f leaves = new Vector3f(0.2f, 0.5f, 0.2f);
                    builder.addBillboard(worldX + 0.5f, height + trunkHeight * 0.7f, worldZ + 0.5f, 1.2f, 1.8f, leaves);
                }
            }
        }
    }

    private float random(int x, int z, int salt) {
        int h = seed ^ salt;
        h ^= x * 0x27d4eb2d;
        h ^= z * 0x165667b1;
        h = Integer.rotateLeft(h, 13);
        h *= 0x85ebca6b;
        h ^= h >>> 16;
        return (h & 0xffff) / 65535.0f;
    }

    private static class MeshDataBuilder {
        private final List<Float> vertices = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();

        public void addCrossQuad(float x, float y, float z, float halfWidth, float height, Vector3f color) {
            addQuad(x, y, z, halfWidth, height, color, 0.0f);
            addQuad(x, y, z, halfWidth, height, color, (float) Math.toRadians(90.0));
        }

        public void addBillboard(float x, float y, float z, float halfWidth, float height, Vector3f color) {
            addQuad(x, y, z, halfWidth, height, color, 0.0f);
        }

        private void addQuad(float x, float y, float z, float halfWidth, float height, Vector3f color, float rotation) {
            float cos = (float) Math.cos(rotation);
            float sin = (float) Math.sin(rotation);
            Vector3f right = new Vector3f(cos, 0.0f, sin).mul(halfWidth);
            Vector3f normal = new Vector3f(-sin, 0.0f, cos).normalize();

            int baseIndex = vertices.size() / 9;

            addVertex(x - right.x, y, z - right.z, normal, color);
            addVertex(x + right.x, y, z + right.z, normal, color);
            addVertex(x + right.x, y + height, z + right.z, normal, color);
            addVertex(x - right.x, y + height, z - right.z, normal, color);

            addQuadIndices(baseIndex);
        }

        public void addBox(float x, float y, float z, float halfWidth, float halfHeight, float halfDepth, Vector3f color) {
            Vector3f[] normals = {
                    new Vector3f(0, 0, 1),
                    new Vector3f(0, 0, -1),
                    new Vector3f(1, 0, 0),
                    new Vector3f(-1, 0, 0),
                    new Vector3f(0, 1, 0),
                    new Vector3f(0, -1, 0)
            };
            float[][] faces = {
                    {-halfWidth, -halfHeight, halfDepth, halfWidth, -halfHeight, halfDepth, halfWidth, halfHeight, halfDepth, -halfWidth, halfHeight, halfDepth},
                    {halfWidth, -halfHeight, -halfDepth, -halfWidth, -halfHeight, -halfDepth, -halfWidth, halfHeight, -halfDepth, halfWidth, halfHeight, -halfDepth},
                    {halfWidth, -halfHeight, halfDepth, halfWidth, -halfHeight, -halfDepth, halfWidth, halfHeight, -halfDepth, halfWidth, halfHeight, halfDepth},
                    {-halfWidth, -halfHeight, -halfDepth, -halfWidth, -halfHeight, halfDepth, -halfWidth, halfHeight, halfDepth, -halfWidth, halfHeight, -halfDepth},
                    {-halfWidth, halfHeight, halfDepth, halfWidth, halfHeight, halfDepth, halfWidth, halfHeight, -halfDepth, -halfWidth, halfHeight, -halfDepth},
                    {-halfWidth, -halfHeight, -halfDepth, halfWidth, -halfHeight, -halfDepth, halfWidth, -halfHeight, halfDepth, -halfWidth, -halfHeight, halfDepth}
            };

            for (int i = 0; i < faces.length; i++) {
                int baseIndex = vertices.size() / 9;
                float[] face = faces[i];
                addVertex(x + face[0], y + face[1], z + face[2], normals[i], color);
                addVertex(x + face[3], y + face[4], z + face[5], normals[i], color);
                addVertex(x + face[6], y + face[7], z + face[8], normals[i], color);
                addVertex(x + face[9], y + face[10], z + face[11], normals[i], color);
                addQuadIndices(baseIndex);
            }
        }

        private void addVertex(float x, float y, float z, Vector3f normal, Vector3f color) {
            vertices.add(x);
            vertices.add(y);
            vertices.add(z);
            vertices.add(normal.x);
            vertices.add(normal.y);
            vertices.add(normal.z);
            vertices.add(color.x);
            vertices.add(color.y);
            vertices.add(color.z);
        }

        private void addQuadIndices(int baseIndex) {
            indices.add(baseIndex);
            indices.add(baseIndex + 1);
            indices.add(baseIndex + 2);
            indices.add(baseIndex);
            indices.add(baseIndex + 2);
            indices.add(baseIndex + 3);
        }

        public FeatureMeshData toMeshData() {
            float[] vertexArray = new float[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) {
                vertexArray[i] = vertices.get(i);
            }
            int[] indexArray = new int[indices.size()];
            for (int i = 0; i < indices.size(); i++) {
                indexArray[i] = indices.get(i);
            }
            return new FeatureMeshData(vertexArray, indexArray);
        }
    }
}
