package com.example.terrain;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

public class FrustumCuller {
    private final FrustumIntersection frustum = new FrustumIntersection();

    public void update(Matrix4f projectionView) {
        frustum.set(projectionView);
    }

    public boolean isVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
