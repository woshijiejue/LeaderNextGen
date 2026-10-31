package cn.leader.ui.clickgui.components.impl;

import cn.leader.module.value.impl.BoolValue;
import cn.leader.ui.clickgui.components.ValueComponent;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import org.jetbrains.skija.Canvas;

public final class BoolComponent extends ValueComponent {
    private static final float HEIGHT = 18.0F;

    public BoolComponent(BoolValue value) {
        super(value);
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    @Override
    public float render(Canvas canvas, float mouseX, float mouseY, float x, float y, float width, float alpha) {
        BoolValue value = (BoolValue) getValue();
        boolean enabled = value.getValue();
        if (hovered(mouseX, mouseY, x, y, width, HEIGHT)) {
            SkiaRender.roundRect(canvas, x, y, width, HEIGHT, 3.0F, SkiaRender.alpha(0xFFFFFFFF, 0.035F * alpha));
        }

        SkiaFont font = Fonts.regular(7.5F);
        float switchWidth = 20.0F;
        float switchX = x + width - switchWidth - 5.0F;
        String labelText = trim(font, value.getName(), switchX - x - 14.0F);
        font.drawStringCenteredY(canvas, labelText, x + 5.0F, y + HEIGHT / 2.0F,
                SkiaRender.alpha(enabled ? 0xFFE9E9EF : 0xFFA8A8B3, alpha), Fonts.shadow());

        float switchHeight = 10.0F;
        float switchY = y + (HEIGHT - switchHeight) / 2.0F;
        int accent = PanelRenderer.accent(0);
        SkiaRender.roundRect(canvas, switchX, switchY, switchWidth, switchHeight, 5.0F,
                SkiaRender.alpha(enabled ? accent : 0xFF555560, alpha * (enabled ? 0.9F : 0.65F)));
        float knobRadius = 3.5F;
        float knobX = enabled ? switchX + switchWidth - knobRadius - 2.0F : switchX + knobRadius + 2.0F;
        SkiaRender.circle(canvas, knobX, switchY + switchHeight / 2.0F, knobRadius, SkiaRender.alpha(0xFFFFFFFF, alpha));
        return lastHeight = HEIGHT;
    }

    private static String trim(SkiaFont font, String text, float maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        String trimmed = text;
        while (!trimmed.isEmpty() && font.getStringWidth(trimmed + "…") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.offsetByCodePoints(trimmed.length(), -1));
        }
        return trimmed.isEmpty() ? "…" : trimmed + "…";
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button, float x, float y, float width) {
        if (button == 0 && hovered(mouseX, mouseY, x, y, width, HEIGHT)) {
            ((BoolValue) getValue()).toggle();
        }
    }
}
