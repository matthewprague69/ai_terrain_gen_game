package com.example.terrain;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class FrustumUtils {
    private FrustumUtils() {
    }

    public static Vector3f[] getFrustumCornersWorldSpace(Matrix4f projectionView) {
        Matrix4f inv = new Matrix4f(projectionView).invert();
        Vector3f[] corners = new Vector3f[8];
        int index = 0;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    Vector4f point = new Vector4f(
                            2.0f * x - 1.0f,
                            2.0f * y - 1.0f,
                            2.0f * z - 1.0f,
                            1.0f
                    );
                    point.mul(inv);
                    point.div(point.w);
                    corners[index++] = new Vector3f(point.x, point.y, point.z);
                }
            }
        }
        return corners;
    }

    public static Matrix4f buildLightView(Vector3f lightDir, Vector3f[] frustumCorners) {
        Vector3f center = new Vector3f();
        for (Vector3f corner : frustumCorners) {
            center.add(corner);
        }
        center.div(frustumCorners.length);

        Vector3f lightPos = new Vector3f(center).sub(new Vector3f(lightDir).mul(200.0f));
        return new Matrix4f().lookAt(lightPos, center, new Vector3f(0.0f, 1.0f, 0.0f));
    }

    public static Matrix4f buildLightOrtho(Vector3f[] frustumCorners, Matrix4f lightView) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        Vector3f cornerView = new Vector3f();
        for (Vector3f corner : frustumCorners) {
            lightView.transformPosition(corner, cornerView);
            minX = Math.min(minX, cornerView.x);
            minY = Math.min(minY, cornerView.y);
            minZ = Math.min(minZ, cornerView.z);
            maxX = Math.max(maxX, cornerView.x);
            maxY = Math.max(maxY, cornerView.y);
            maxZ = Math.max(maxZ, cornerView.z);
        }

        return new Matrix4f().ortho(minX, maxX, minY, maxY, -maxZ - 50.0f, -minZ + 50.0f);
    }
}
