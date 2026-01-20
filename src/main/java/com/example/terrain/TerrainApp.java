package com.example.terrain;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class TerrainApp {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int CHUNK_SIZE = 64;
    private static final int RENDER_DISTANCE = 4;
    private static final int FEATURE_DISTANCE = 3;
    private static final int SHADOW_MAP_SIZE = 2048;
    private static final int CASCADE_COUNT = 3;

    private long window;
    private Camera camera;
    private ShaderProgram shader;
    private ChunkManager chunkManager;
    private FeatureManager featureManager;
    private FrustumCuller frustumCuller;
    private TextureArray terrainTextures;
    private ShadowMap shadowMap;
    private ShaderProgram shadowShader;
    private ShaderProgram featureShader;
    private final Matrix4f[] cascadeMatrices = new Matrix4f[CASCADE_COUNT];
    private final float[] cascadeSplits = new float[CASCADE_COUNT];
    private double lastTime;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "AI Terrain Generator - Increment 6", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Failed to create GLFW window");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.08f, 0.1f, 0.12f, 1.0f);

        Input.init(window);
        camera = new Camera(new Vector3f(0.0f, 18.0f, 32.0f));

        shader = new ShaderProgram("shaders/terrain.vert", "shaders/terrain.frag");
        shadowShader = new ShaderProgram("shaders/shadow.vert", "shaders/shadow.frag");
        featureShader = new ShaderProgram("shaders/feature.vert", "shaders/feature.frag");
        TerrainGenerator generator = new TerrainGenerator(1337);
        chunkManager = new ChunkManager(generator, CHUNK_SIZE, RENDER_DISTANCE);
        featureManager = new FeatureManager(generator, CHUNK_SIZE, FEATURE_DISTANCE, 1337);
        frustumCuller = new FrustumCuller();
        terrainTextures = TextureArray.createDefault();
        shadowMap = new ShadowMap(SHADOW_MAP_SIZE, CASCADE_COUNT);
        for (int i = 0; i < CASCADE_COUNT; i++) {
            cascadeMatrices[i] = new Matrix4f();
        }

        lastTime = glfwGetTime();
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            double current = glfwGetTime();
            float deltaTime = (float) (current - lastTime);
            lastTime = current;

            if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }

            Input.update(window, deltaTime, camera);
            chunkManager.update(camera.getPosition());
            featureManager.update(camera.getPosition());

            updateCascades();
            renderShadowPass();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            shader.bind();
            Matrix4f projection = camera.getProjectionMatrix(getAspectRatio());
            Matrix4f view = camera.getViewMatrix();
            Matrix4f projectionView = new Matrix4f(projection).mul(view);
            shader.setMatrix4f("uProjection", projection);
            shader.setMatrix4f("uView", view);
            shader.setMatrix4fArray("uLightViewProj", cascadeMatrices);
            shader.setFloatArray("uCascadeSplits", cascadeSplits);
            shader.setVector3f("uLightDir", new Vector3f(-0.4f, -1.0f, -0.3f));
            shader.setVector3f("uBaseColor", new Vector3f(0.2f, 0.7f, 0.25f));
            shader.setVector3f("uCameraPos", camera.getPosition());
            shader.setVector3f("uFogColor", new Vector3f(0.7f, 0.8f, 0.9f));
            shader.setFloat("uFogStart", 80.0f);
            shader.setFloat("uFogEnd", 200.0f);
            shader.setFloat("uShadowBias", 0.0015f);
            shader.setInt("uTerrainTextures", 0);
            shader.setInt("uShadowMap", 1);
            shader.setVector3fArray("uBiomeTextureWeights", new Vector3f[]{
                    BiomeType.GRASSLAND.getTextureWeights(),
                    BiomeType.HIGHLANDS.getTextureWeights(),
                    BiomeType.TUNDRA.getTextureWeights()
            });

            terrainTextures.bind(0);
            shadowMap.bindForReading(1);

            frustumCuller.update(projectionView);

            for (Chunk chunk : chunkManager.getChunks()) {
                ChunkMesh mesh = chunk.getMesh();
                if (mesh == null) {
                    continue;
                }
                float minX = chunk.getChunkX() * CHUNK_SIZE;
                float maxX = minX + CHUNK_SIZE;
                float minZ = chunk.getChunkZ() * CHUNK_SIZE;
                float maxZ = minZ + CHUNK_SIZE;
                if (!frustumCuller.isVisible(minX, chunk.getMinY(), minZ, maxX, chunk.getMaxY(), maxZ)) {
                    continue;
                }
                mesh.draw();
            }

            shader.unbind();

            renderFeatures(projection, view);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private float getAspectRatio() {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        return (float) width[0] / (float) height[0];
    }

    private void cleanup() {
        chunkManager.shutdown();
        featureManager.shutdown();
        shader.dispose();
        shadowShader.dispose();
        featureShader.dispose();
        terrainTextures.dispose();
        shadowMap.dispose();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void renderFeatures(Matrix4f projection, Matrix4f view) {
        featureShader.bind();
        featureShader.setMatrix4f("uProjection", projection);
        featureShader.setMatrix4f("uView", view);
        featureShader.setMatrix4fArray("uLightViewProj", cascadeMatrices);
        featureShader.setFloatArray("uCascadeSplits", cascadeSplits);
        featureShader.setVector3f("uLightDir", new Vector3f(-0.4f, -1.0f, -0.3f));
        featureShader.setVector3f("uCameraPos", camera.getPosition());
        featureShader.setVector3f("uFogColor", new Vector3f(0.7f, 0.8f, 0.9f));
        featureShader.setFloat("uFogStart", 60.0f);
        featureShader.setFloat("uFogEnd", 180.0f);
        featureShader.setFloat("uShadowBias", 0.0015f);
        featureShader.setInt("uShadowMap", 1);
        shadowMap.bindForReading(1);

        Vector3f cameraPos = camera.getPosition();
        float nearThreshold = CHUNK_SIZE * 2.5f;
        for (FeatureChunk chunk : featureManager.getChunks()) {
            float centerX = (chunk.getChunkX() + 0.5f) * CHUNK_SIZE;
            float centerZ = (chunk.getChunkZ() + 0.5f) * CHUNK_SIZE;
            float dx = centerX - cameraPos.x;
            float dz = centerZ - cameraPos.z;
            float distSq = dx * dx + dz * dz;
            FeatureMesh mesh = distSq < nearThreshold * nearThreshold ? chunk.getNearMesh() : chunk.getFarMesh();
            if (mesh != null) {
                mesh.draw();
            }
        }

        featureShader.unbind();
    }

    private void updateCascades() {
        float aspect = getAspectRatio();
        float near = camera.getNearPlane();
        float far = camera.getFarPlane();
        float lambda = 0.65f;
        float clipRange = far - near;
        float minZ = near;
        float maxZ = near + clipRange;
        float range = maxZ - minZ;
        float ratio = maxZ / minZ;

        for (int i = 0; i < CASCADE_COUNT; i++) {
            float p = (i + 1) / (float) CASCADE_COUNT;
            float log = minZ * (float) Math.pow(ratio, p);
            float uniform = minZ + range * p;
            float split = lambda * (log - uniform) + uniform;
            cascadeSplits[i] = split;
        }

        Matrix4f view = camera.getViewMatrix();
        Vector3f lightDir = new Vector3f(-0.4f, -1.0f, -0.3f).normalize();
        float lastSplit = near;
        for (int i = 0; i < CASCADE_COUNT; i++) {
            float splitDist = cascadeSplits[i];
            Matrix4f proj = camera.buildProjection(aspect, lastSplit, splitDist);
            Matrix4f projView = new Matrix4f(proj).mul(view);

            Vector3f[] corners = FrustumUtils.getFrustumCornersWorldSpace(projView);
            Matrix4f lightView = FrustumUtils.buildLightView(lightDir, corners);
            Matrix4f lightProj = FrustumUtils.buildLightOrtho(corners, lightView);
            cascadeMatrices[i].set(lightProj.mul(lightView));

            lastSplit = splitDist;
        }
    }

    private void renderShadowPass() {
        glCullFace(GL_FRONT);
        shadowShader.bind();
        for (int i = 0; i < CASCADE_COUNT; i++) {
            shadowMap.bindForWriting(i);
            shadowShader.setMatrix4f("uLightViewProj", cascadeMatrices[i]);
            for (Chunk chunk : chunkManager.getChunks()) {
                ChunkMesh mesh = chunk.getMesh();
                if (mesh != null) {
                    mesh.draw();
                }
            }
        }
        shadowShader.unbind();
        glCullFace(GL_BACK);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        int[] size = getFramebufferSize();
        glViewport(0, 0, size[0], size[1]);
    }

    private int[] getFramebufferSize() {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);
        return new int[]{width[0], height[0]};
    }
}
