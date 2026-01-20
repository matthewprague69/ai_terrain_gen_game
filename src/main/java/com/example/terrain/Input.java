package com.example.terrain;

import org.lwjgl.glfw.GLFWCursorPosCallback;

import static org.lwjgl.glfw.GLFW.*;

public final class Input {
    private static boolean firstMouse = true;
    private static double lastX;
    private static double lastY;
    private static double deltaX;
    private static double deltaY;

    private Input() {
    }

    public static void init(long window) {
        glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long windowHandle, double xpos, double ypos) {
                if (firstMouse) {
                    lastX = xpos;
                    lastY = ypos;
                    firstMouse = false;
                }
                deltaX += xpos - lastX;
                deltaY += ypos - lastY;
                lastX = xpos;
                lastY = ypos;
            }
        });
    }

    public static void update(long window, float deltaTime, Camera camera) {
        float moveSpeed = 5.0f;
        float mouseSensitivity = 0.1f;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            camera.moveForward(moveSpeed * deltaTime);
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            camera.moveForward(-moveSpeed * deltaTime);
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            camera.moveRight(-moveSpeed * deltaTime);
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            camera.moveRight(moveSpeed * deltaTime);
        }
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            camera.moveUp(moveSpeed * deltaTime);
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            camera.moveUp(-moveSpeed * deltaTime);
        }

        float yawDelta = (float) deltaX * mouseSensitivity;
        float pitchDelta = (float) deltaY * mouseSensitivity;
        deltaX = 0.0;
        deltaY = 0.0;

        camera.rotate(yawDelta, pitchDelta);
    }
}
