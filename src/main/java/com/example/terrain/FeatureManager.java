package com.example.terrain;

import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeatureManager {
    private final Map<Long, FeatureChunk> chunks = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<UploadCommand> uploadQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor;
    private final FeatureMeshBuilder builder;
    private final int chunkSize;
    private final int featureDistance;

    public FeatureManager(TerrainGenerator generator, int chunkSize, int featureDistance, int seed) {
        this.chunkSize = chunkSize;
        this.featureDistance = featureDistance;
        this.builder = new FeatureMeshBuilder(generator, chunkSize, seed);
        int threadCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public void update(Vector3f cameraPosition) {
        int cameraChunkX = (int) Math.floor(cameraPosition.x / chunkSize);
        int cameraChunkZ = (int) Math.floor(cameraPosition.z / chunkSize);

        Set<Long> needed = new HashSet<>();
        for (int dz = -featureDistance; dz <= featureDistance; dz++) {
            for (int dx = -featureDistance; dx <= featureDistance; dx++) {
                int chunkX = cameraChunkX + dx;
                int chunkZ = cameraChunkZ + dz;
                long key = pack(chunkX, chunkZ);
                needed.add(key);
                if (!chunks.containsKey(key)) {
                    FeatureChunk chunk = new FeatureChunk(chunkX, chunkZ);
                    chunks.put(key, chunk);
                    submitBuild(chunkX, chunkZ);
                }
            }
        }

        chunks.entrySet().removeIf(entry -> {
            if (!needed.contains(entry.getKey())) {
                entry.getValue().dispose();
                return true;
            }
            return false;
        });

        UploadCommand command;
        while ((command = uploadQueue.poll()) != null) {
            FeatureChunk chunk = chunks.get(pack(command.chunkX(), command.chunkZ()));
            if (chunk != null) {
                FeatureMesh nearMesh = FeatureMesh.upload(command.nearMesh());
                FeatureMesh farMesh = FeatureMesh.upload(command.farMesh());
                chunk.setMeshes(nearMesh, farMesh);
            }
        }
    }

    public Iterable<FeatureChunk> getChunks() {
        return chunks.values();
    }

    public void shutdown() {
        executor.shutdownNow();
        for (FeatureChunk chunk : chunks.values()) {
            chunk.dispose();
        }
        chunks.clear();
    }

    private void submitBuild(int chunkX, int chunkZ) {
        executor.submit(() -> {
            FeatureMeshData near = builder.buildNear(chunkX, chunkZ);
            FeatureMeshData far = builder.buildFar(chunkX, chunkZ);
            uploadQueue.add(new UploadCommand(chunkX, chunkZ, near, far));
        });
    }

    private long pack(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    public record UploadCommand(int chunkX, int chunkZ, FeatureMeshData nearMesh, FeatureMeshData farMesh) {
    }
}
