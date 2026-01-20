package com.example.terrain;

public record ChunkMeshData(float[] vertices, int[] indices, float minY, float maxY, int lod) {
}
