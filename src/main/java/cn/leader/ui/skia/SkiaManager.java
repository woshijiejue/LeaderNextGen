package cn.leader.ui.skia;

import cn.leader.event.impl.RenderSkiaEvent;
import cn.leader.util.IMinecraft;

public class SkiaManager implements IMinecraft {
    public final Skia skia = new Skia();

    public SkiaManager() {
        instance.getEventManager().register(this);
    }

    public void render() {
        // Minecraft 26.2 replaced the old OpenGL framebuffer API with GPU surfaces.
        // The regular HUD is rendered through GuiGraphicsExtractor; keep this optional
        // compatibility hook inert until a GPU-backed Skia surface is requested.
    }

    public void destroy() {
        skia.cleanup();
    }
}
