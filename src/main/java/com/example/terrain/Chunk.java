package com.example.terrain;

public class Chunk {
    private final int chunkX;
    private final int chunkZ;
    private ChunkMesh mesh;
    private float minY;
    private float maxY;
    private int lod;
    private int requestedLod;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public ChunkMesh getMesh() {
        return mesh;
    }

    public void setMesh(ChunkMesh mesh, float minY, float maxY, int lod) {
        this.mesh = mesh;
        this.minY = minY;
        this.maxY = maxY;
        this.lod = lod;
        this.requestedLod = lod;
    }

    public float getMinY() {
        return minY;
    }

    public float getMaxY() {
        return maxY;
    }

    public int getLod() {
        return lod;
    }

    public int getRequestedLod() {
        return requestedLod;
    }

    public void setRequestedLod(int requestedLod) {
        this.requestedLod = requestedLod;
    }

    public void dispose() {
        if (mesh != null) {
            mesh.dispose();
        }
    }
}
