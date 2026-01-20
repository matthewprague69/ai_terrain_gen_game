package com.example.terrain;

public class FeatureChunk {
    private final int chunkX;
    private final int chunkZ;
    private FeatureMesh nearMesh;
    private FeatureMesh farMesh;

    public FeatureChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public FeatureMesh getNearMesh() {
        return nearMesh;
    }

    public FeatureMesh getFarMesh() {
        return farMesh;
    }

    public void setMeshes(FeatureMesh nearMesh, FeatureMesh farMesh) {
        this.nearMesh = nearMesh;
        this.farMesh = farMesh;
    }

    public void dispose() {
        if (nearMesh != null) {
            nearMesh.dispose();
        }
        if (farMesh != null) {
            farMesh.dispose();
        }
    }
}
