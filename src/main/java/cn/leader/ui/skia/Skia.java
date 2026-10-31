package cn.leader.ui.skia;

import cn.leader.util.IMinecraft;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import org.jetbrains.skija.*;
import org.lwjgl.opengl.*;

public class Skia implements IMinecraft {
    public BackendRenderTarget renderTarget;
    public DirectContext context;
    public Surface surface;
    public Canvas canvas;

    private int fbo = -1;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private int lastTexId = -1;

    private final int[] savedViewport = new int[4];
    private final int[] savedColorMask = new int[4];
    private final int[] savedState = new int[20];
    private final boolean[] savedCaps = new boolean[4];

    public int getWidth() {
        return lastWidth;
    }

    public int getHeight() {
        return lastHeight;
    }

    public boolean isAvailable() {
        return currentTexture() != null;
    }

    private GlTexture currentTexture() {
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        if (target == null) return null;
        GpuTexture texture = target.getColorTexture();
        return texture instanceof GlTexture glTexture ? glTexture : null;
    }

    public void initSkia() {
        if (context == null) {
            context = DirectContext.makeGL();
        }
        createSurface();
    }

    private void createSurface() {
        if (surface != null) {
            surface.close();
        }

        if (renderTarget != null) {
            renderTarget.close();
        }

        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        GlTexture texture = currentTexture();
        int width = target.width;
        int height = target.height;
        int texId = texture.glId();

        int previousFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        if (fbo == -1) {
            fbo = GL30.glGenFramebuffers();
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texId, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);

        renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, fbo, 0x8058);
        surface = Surface.makeFromBackendRenderTarget(context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB());
        canvas = surface.getCanvas();

        lastWidth = width;
        lastHeight = height;
        lastTexId = texId;
    }

    private void saveState() {
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, savedViewport);
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, savedColorMask);
        savedState[0] = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        savedState[1] = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        savedState[2] = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        savedState[3] = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        savedState[4] = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        savedState[5] = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        savedState[6] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        savedState[7] = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        savedState[8] = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        savedState[9] = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        savedState[10] = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        savedState[11] = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        savedState[12] = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        savedState[13] = GL11.glGetInteger(GL11.GL_DEPTH_WRITEMASK);
        savedState[14] = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        savedState[15] = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        savedState[16] = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        savedState[17] = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        savedState[18] = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        savedState[19] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        savedCaps[0] = GL11.glIsEnabled(GL11.GL_BLEND);
        savedCaps[1] = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        savedCaps[2] = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        savedCaps[3] = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    }

    private static void setCap(int cap, boolean enabled) {
        if (enabled) {
            GL11.glEnable(cap);
        } else {
            GL11.glDisable(cap);
        }
    }

    private void restoreState() {
        GL20.glUseProgram(savedState[0]);
        GL30.glBindVertexArray(savedState[1]);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedState[2]);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, savedState[3]);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, savedState[4]);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, savedState[6]);
        GL33.glBindSampler(0, savedState[19]);
        GL13.glActiveTexture(savedState[5]);
        GL14.glBlendFuncSeparate(savedState[7], savedState[8], savedState[9], savedState[10]);
        GL20.glBlendEquationSeparate(savedState[11], savedState[12]);
        GL11.glDepthMask(savedState[13] != 0);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, savedState[14]);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, savedState[15]);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, savedState[16]);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, savedState[17]);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, savedState[18]);
        setCap(GL11.GL_BLEND, savedCaps[0]);
        setCap(GL11.GL_DEPTH_TEST, savedCaps[1]);
        setCap(GL11.GL_CULL_FACE, savedCaps[2]);
        setCap(GL11.GL_SCISSOR_TEST, savedCaps[3]);
        GL11.glColorMask(savedColorMask[0] != 0, savedColorMask[1] != 0, savedColorMask[2] != 0, savedColorMask[3] != 0);
        GL11.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
    }

    public void beginFrame() {
        RenderSystem.assertOnRenderThread();
        saveState();

        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, lastWidth, lastHeight);
        GL11.glColorMask(false, false, false, true);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glColorMask(true, true, true, true);

        if (context != null) {
            context.resetGLAll();
        }
    }

    public void endFrame() {
        RenderSystem.assertOnRenderThread();

        if (surface != null) {
            surface.flushAndSubmit();
        }

        restoreState();
    }

    public void checkAndUpdateSurface() {
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        GlTexture texture = currentTexture();
        if (lastWidth != target.width || lastHeight != target.height || lastTexId != texture.glId()) {
            createSurface();
        }
    }

    public void cleanup() {
        if (surface != null) {
            surface.close();
        }
        if (renderTarget != null) {
            renderTarget.close();
        }
        if (context != null) {
            context.abandon();
        }
        if (fbo != -1) {
            GL30.glDeleteFramebuffers(fbo);
        }

        surface = null;
        renderTarget = null;
        context = null;
        canvas = null;
        fbo = -1;
    }
}
