package com.example.terrain;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL46.*;

public class TextureArray {
    private final int textureId;

    private TextureArray(int textureId) {
        this.textureId = textureId;
    }

    public static TextureArray createDefault() {
        int size = 32;
        int layers = 3;
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureId);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, size, size, layers, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        uploadLayer(0, size, size, new byte[]{(byte) 90, (byte) 140, (byte) 60, (byte) 255});
        uploadLayer(1, size, size, new byte[]{(byte) 120, (byte) 120, (byte) 120, (byte) 255});
        uploadLayer(2, size, size, new byte[]{(byte) 230, (byte) 230, (byte) 230, (byte) 255});

        glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);

        return new TextureArray(textureId);
    }

    private static void uploadLayer(int layer, int width, int height, byte[] baseColor) {
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean checker = ((x / 4) + (y / 4)) % 2 == 0;
                byte r = baseColor[0];
                byte g = baseColor[1];
                byte b = baseColor[2];
                byte a = baseColor[3];
                if (checker) {
                    r = (byte) Math.min(255, (r & 0xFF) + 20);
                    g = (byte) Math.min(255, (g & 0xFF) + 20);
                    b = (byte) Math.min(255, (b & 0xFF) + 20);
                }
                buffer.put(r).put(g).put(b).put(a);
            }
        }
        buffer.flip();
        glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, width, height, 1, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        MemoryUtil.memFree(buffer);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, textureId);
    }

    public void dispose() {
        glDeleteTextures(textureId);
    }
}
