package cn.leader.ui.skia;

import cn.leader.event.impl.RenderSkiaEvent;
import cn.leader.util.IMinecraft;

public class SkiaManager implements IMinecraft {
    public final Skia skia = new Skia();

    public SkiaManager() {
        instance.getEventManager().register(this);
    }

    public void render() {
        if (skia.context == null) skia.initSkia();
        skia.checkAndUpdateSurface();
        skia.beginFrame();
        skia.canvas.save();
        skia.canvas.scale((float) mc.getWindow().getScaleFactor(), (float) mc.getWindow().getScaleFactor());
        RenderSkiaEvent event = new RenderSkiaEvent(skia.canvas);
        instance.getEventManager().call(event);
        skia.canvas.restore();
        skia.endFrame();
    }

    public void destroy() {
        skia.cleanup();
    }
}