package com.example.terrain;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Vector3f position;
    private final Vector3f front;
    private final Vector3f up;
    private final Vector3f right;
    private final Vector3f worldUp;

    private float yaw;
    private float pitch;
    private float fovRadians = (float) Math.toRadians(60.0f);
    private float nearPlane = 0.1f;
    private float farPlane = 500.0f;

    public Camera(Vector3f position) {
        this.position = new Vector3f(position);
        this.front = new Vector3f(0.0f, 0.0f, -1.0f);
        this.worldUp = new Vector3f(0.0f, 1.0f, 0.0f);
        this.up = new Vector3f(worldUp);
        this.right = new Vector3f();
        this.yaw = -90.0f;
        this.pitch = 0.0f;
        updateVectors();
    }

    public void moveForward(float amount) {
        position.fma(amount, front);
    }

    public void moveRight(float amount) {
        position.fma(amount, right);
    }

    public void moveUp(float amount) {
        position.fma(amount, worldUp);
    }

    public void rotate(float yawOffset, float pitchOffset) {
        yaw += yawOffset;
        pitch -= pitchOffset;

        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }
        updateVectors();
    }

    public Matrix4f getViewMatrix() {
        Vector3f center = new Vector3f(position).add(front);
        return new Matrix4f().lookAt(position, center, up);
    }

    public Matrix4f getProjectionMatrix(float aspectRatio) {
        return buildProjection(aspectRatio, nearPlane, farPlane);
    }

    public Matrix4f buildProjection(float aspectRatio, float near, float far) {
        return new Matrix4f().perspective(fovRadians, aspectRatio, near, far);
    }

    public float getNearPlane() {
        return nearPlane;
    }

    public float getFarPlane() {
        return farPlane;
    }

    public float getFovRadians() {
        return fovRadians;
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    private void updateVectors() {
        front.set(
                (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)))
        ).normalize();

        right.set(front).cross(worldUp).normalize();
        up.set(right).cross(front).normalize();
    }
}
