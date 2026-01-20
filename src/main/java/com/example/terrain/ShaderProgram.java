package com.example.terrain;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL46.*;

public class ShaderProgram {
    private final int programId;

    public ShaderProgram(String vertexPath, String fragmentPath) {
        int vertexId = createShader(vertexPath, GL_VERTEX_SHADER);
        int fragmentId = createShader(fragmentPath, GL_FRAGMENT_SHADER);

        programId = glCreateProgram();
        glAttachShader(programId, vertexId);
        glAttachShader(programId, fragmentId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new IllegalStateException("Shader program linking failed: " + glGetProgramInfoLog(programId));
        }

        glDetachShader(programId, vertexId);
        glDetachShader(programId, fragmentId);
        glDeleteShader(vertexId);
        glDeleteShader(fragmentId);
    }

    private int createShader(String path, int type) {
        String source = loadResource(path);
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new IllegalStateException("Shader compile failed for " + path + ": " + glGetShaderInfoLog(shaderId));
        }
        return shaderId;
    }

    private String loadResource(String path) {
        try (InputStream stream = ShaderProgram.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Shader resource not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }
                return builder.toString();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load shader: " + path, e);
        }
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setMatrix4f(String name, Matrix4f matrix) {
        int location = glGetUniformLocation(programId, name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    public void setMatrix4fArray(String name, Matrix4f[] matrices) {
        int location = glGetUniformLocation(programId, name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16 * matrices.length);
            for (Matrix4f matrix : matrices) {
                matrix.get(buffer);
            }
            buffer.flip();
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    public void setVector3f(String name, Vector3f value) {
        int location = glGetUniformLocation(programId, name);
        glUniform3f(location, value.x, value.y, value.z);
    }

    public void setVector3fArray(String name, Vector3f[] values) {
        int location = glGetUniformLocation(programId, name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length * 3);
            for (Vector3f value : values) {
                buffer.put(value.x).put(value.y).put(value.z);
            }
            buffer.flip();
            glUniform3fv(location, buffer);
        }
    }

    public void setInt(String name, int value) {
        int location = glGetUniformLocation(programId, name);
        glUniform1i(location, value);
    }

    public void setFloat(String name, float value) {
        int location = glGetUniformLocation(programId, name);
        glUniform1f(location, value);
    }

    public void setFloatArray(String name, float[] values) {
        int location = glGetUniformLocation(programId, name);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length);
            buffer.put(values).flip();
            glUniform1fv(location, buffer);
        }
    }

    public void dispose() {
        glDeleteProgram(programId);
    }
}
