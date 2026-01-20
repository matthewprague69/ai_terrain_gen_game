package com.example.terrain;

import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChunkManager {
    private final Map<Long, Chunk> chunks = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ChunkUploadCommand> uploadQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor;
    private final ChunkMeshBuilder meshBuilder;
    private final int chunkSize;
    private final int renderDistance;
    private final int lodSwitchDistance = 2;

    public ChunkManager(TerrainGenerator generator, int chunkSize, int renderDistance) {
        this.chunkSize = chunkSize;
        this.renderDistance = renderDistance;
        this.meshBuilder = new ChunkMeshBuilder(generator, chunkSize);
        int threadCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public void update(Vector3f cameraPosition) {
        int cameraChunkX = (int) Math.floor(cameraPosition.x / chunkSize);
        int cameraChunkZ = (int) Math.floor(cameraPosition.z / chunkSize);

        Set<Long> needed = new HashSet<>();
        for (int dz = -renderDistance; dz <= renderDistance; dz++) {
            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                int chunkX = cameraChunkX + dx;
                int chunkZ = cameraChunkZ + dz;
                long key = pack(chunkX, chunkZ);
                needed.add(key);
                int lod = selectLod(dx, dz);
                if (!chunks.containsKey(key)) {
                    Chunk chunk = new Chunk(chunkX, chunkZ);
                    chunk.setRequestedLod(lod);
                    chunks.put(key, chunk);
                    submitBuild(chunkX, chunkZ, lod);
                } else {
                    Chunk chunk = chunks.get(key);
                    if (chunk != null && chunk.getRequestedLod() != lod) {
                        chunk.setRequestedLod(lod);
                        submitBuild(chunkX, chunkZ, lod);
                    }
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

        ChunkUploadCommand command;
        while ((command = uploadQueue.poll()) != null) {
            Chunk chunk = chunks.get(pack(command.chunkX(), command.chunkZ()));
            if (chunk != null) {
                ChunkMesh mesh = ChunkMesh.upload(command.meshData());
                chunk.setMesh(mesh, command.meshData().minY(), command.meshData().maxY(), command.meshData().lod());
            }
        }
    }

    public Iterable<Chunk> getChunks() {
        return chunks.values();
    }

    public void shutdown() {
        executor.shutdownNow();
        for (Chunk chunk : chunks.values()) {
            chunk.dispose();
        }
        chunks.clear();
    }

    private void submitBuild(int chunkX, int chunkZ, int lod) {
        executor.submit(() -> {
            ChunkMeshData data = meshBuilder.build(chunkX, chunkZ, lod);
            uploadQueue.add(new ChunkUploadCommand(chunkX, chunkZ, data));
        });
    }

    private long pack(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    private int selectLod(int dx, int dz) {
        int distance = Math.max(Math.abs(dx), Math.abs(dz));
        return distance > lodSwitchDistance ? 1 : 0;
    }

    public record ChunkUploadCommand(int chunkX, int chunkZ, ChunkMeshData meshData) {
    }
}
