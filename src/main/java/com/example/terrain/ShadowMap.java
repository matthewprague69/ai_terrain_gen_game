package com.example.terrain;

import static org.lwjgl.opengl.GL46.*;

public class ShadowMap {
    private final int size;
    private final int layers;
    private final int depthTextureId;
    private final int fboId;

    public ShadowMap(int size, int layers) {
        this.size = size;
        this.layers = layers;
        this.depthTextureId = glGenTextures();
        this.fboId = glGenFramebuffers();

        glBindTexture(GL_TEXTURE_2D_ARRAY, depthTextureId);
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_DEPTH_COMPONENT32F, size, size, layers, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);

        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthTextureId, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Shadow map framebuffer incomplete");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bindForWriting(int layer) {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthTextureId, 0, layer);
        glViewport(0, 0, size, size);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    public void bindForReading(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, depthTextureId);
    }

    public void dispose() {
        glDeleteFramebuffers(fboId);
        glDeleteTextures(depthTextureId);
    }
}
