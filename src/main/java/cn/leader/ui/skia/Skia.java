package cn.leader.ui.skia;

import cn.leader.util.IMinecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.skija.*;
import org.lwjgl.opengl.*;

public class Skia implements IMinecraft {
    public BackendRenderTarget renderTarget;
    public DirectContext context;
    public Surface surface;
    public Canvas canvas;

    private int lastWidth = -1;
    private int lastHeight = -1;
    private int lastFbId = -1;

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

        int width = mc.getFramebuffer().textureWidth;
        int height = mc.getFramebuffer().textureHeight;
        int fbId = mc.getFramebuffer().fbo;

        renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, fbId, 0x8058);
        surface = Surface.makeFromBackendRenderTarget(context, renderTarget, SurfaceOrigin.BOTTOM_LEFT, SurfaceColorFormat.RGBA_8888, ColorSpace.getSRGB());
        canvas = surface.getCanvas();

        lastWidth = width;
        lastHeight = height;
        lastFbId = fbId;
    }

    public void beginFrame() {
        RenderSystem.assertOnRenderThread();

        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glClearColor(0f, 0f, 0f, 0f);

        RenderSystem.colorMask(false, false, false, true);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        RenderSystem.colorMask(true, true, true, true);

        if (context != null) {
            context.resetGLAll();
        }
    }

    public void endFrame() {
        RenderSystem.assertOnRenderThread();

        if (surface != null) {
            surface.flushAndSubmit();
        }

        GL33.glBindSampler(0, 0);

        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_BLEND);

        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderSystem.blendEquation(GL14.GL_FUNC_ADD);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);

        RenderSystem.colorMask(true, true, true, true);
        GL11.glColorMask(true, true, true, true);

        RenderSystem.depthMask(true);
        GL11.glDepthMask(true);

        RenderSystem.disableScissor();
        RenderSystem.disableDepthTest();
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);

        RenderSystem.disableCull();
    }

    public void checkAndUpdateSurface() {
        if (lastWidth != mc.getFramebuffer().textureWidth || lastHeight != mc.getFramebuffer().textureHeight || lastFbId != mc.getFramebuffer().fbo) {
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

        surface = null;
        renderTarget = null;
        context = null;
        canvas = null;
    }
}