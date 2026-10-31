package cn.leader.util.render;

import cn.leader.module.impl.visual.HUD;
import cn.leader.module.impl.visual.PostProcessing;
import cn.leader.util.IMinecraft;
import org.jetbrains.skija.Canvas;

public final class PanelRenderer implements IMinecraft {

    public static float radius() {
        HUD hud = instance.getModuleManager().getModule(HUD.class);
        return hud == null ? 6.0F : hud.radius.getValue();
    }

    public static int background(float alpha) {
        HUD hud = instance.getModuleManager().getModule(HUD.class);
        float base = hud == null ? 0.55F : hud.backgroundAlpha.getValue();
        return SkiaRender.alpha(0xFF101014, base * alpha);
    }

    public static void panel(Canvas canvas, float x, float y, float width, float height) {
        panel(canvas, x, y, width, height, radius(), 1.0F, 0, true);
    }

    public static void panel(Canvas canvas, float x, float y, float width, float height, float radius, float alpha) {
        panel(canvas, x, y, width, height, radius, alpha, 0, true);
    }

    public static void panel(Canvas canvas, float x, float y, float width, float height, float radius, float alpha, int glowColor) {
        panel(canvas, x, y, width, height, radius, alpha, glowColor, true);
    }

    public static void panel(Canvas canvas, float x, float y, float width, float height, float radius,
                             float alpha, int glowColor, boolean postProcessing) {
        if (width <= 0 || height <= 0 || alpha <= 0.01F) return;

        PostProcessing post = instance.getModuleManager().getModule(PostProcessing.class);
        boolean effects = postProcessing && post != null && post.isEnabled();
        boolean blur = effects && post.blurActive() && instance.getSkiaManager().backdrop().isReady();

        if (effects && (post.shadowActive() || post.glowActive())) {
            canvas.save();
            SkiaRender.excludeRoundRect(canvas, x, y, width, height, radius);
            if (post.shadowActive()) {
                float spread = post.shadowRadius.getValue();
                SkiaRender.blurredRoundRect(canvas, x, y + 1.0F, width, height, radius, spread,
                        SkiaRender.alpha(0xFF000000, post.shadowAlpha.getValue() * alpha));
            }
            if (post.glowActive()) {
                int color = glowColor == 0 ? accent(0) : glowColor;
                SkiaRender.blurredRoundRect(canvas, x, y, width, height, radius, post.glowRadius.getValue(),
                        SkiaRender.alpha(color | 0xFF000000, post.glowAlpha.getValue() * alpha));
            }
            canvas.restore();
        }

        if (blur) {
            instance.getSkiaManager().backdrop().draw(canvas, x, y, width, height, radius,
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), alpha);
        }

        SkiaRender.roundRect(canvas, x, y, width, height, radius, background(alpha));
        SkiaRender.roundRectOutline(canvas, x + 0.25F, y + 0.25F, width - 0.5F, height - 0.5F, radius, 0.5F,
                SkiaRender.alpha(0xFFFFFFFF, 0.06F * alpha));
    }

    public static int accent(int index) {
        HUD hud = instance.getModuleManager().getModule(HUD.class);
        return hud == null ? 0xFF8A5CFF : hud.getColor(index);
    }

    private PanelRenderer() {
    }
}
