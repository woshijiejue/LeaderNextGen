package cn.leader.ui.skia;

import cn.leader.event.impl.RenderSkiaEvent;
import cn.leader.module.impl.visual.PostProcessing;
import cn.leader.ui.clickgui.ClickGUIScreen;
import cn.leader.util.IMinecraft;

public class SkiaManager implements IMinecraft {
    public final Skia skia = new Skia();
    private final Backdrop backdrop = new Backdrop();

    public SkiaManager() {
        instance.getEventManager().register(this);
    }

    public Backdrop backdrop() {
        return backdrop;
    }

    public void render(RenderSkiaEvent.Layer layer) {
        if (!skia.isAvailable()) return;
        if (layer == RenderSkiaEvent.Layer.SCREEN && !(mc.gui.screen() instanceof ClickGUIScreen)) return;

        if (skia.context == null) skia.initSkia();
        skia.checkAndUpdateSurface();
        skia.beginFrame();

        try {
            PostProcessing post = instance.getModuleManager().getModule(PostProcessing.class);
            if (post != null && post.blurActive()) {
                backdrop.capture(skia, post.downsample(), post.blurRadius.getValue());
            } else {
                backdrop.release();
            }

            float scale = (float) mc.getWindow().getGuiScale();
            skia.canvas.save();
            try {
                skia.canvas.scale(scale, scale);

                if (layer == RenderSkiaEvent.Layer.SCREEN) {
                    ((ClickGUIScreen) mc.gui.screen()).renderSkia(skia.canvas);
                } else {
                    instance.getEventManager().call(new RenderSkiaEvent(skia.canvas, layer));
                }
            } finally {
                skia.canvas.restore();
            }
        } finally {
            backdrop.release();
            skia.endFrame();
        }
    }

    public void destroy() {
        backdrop.destroy();
        skia.cleanup();
    }
}
