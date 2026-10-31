package cn.leader.ui.clickgui.components.impl;

import cn.leader.module.value.impl.NumberValue;
import cn.leader.ui.clickgui.components.ValueComponent;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import org.jetbrains.skija.Canvas;

public final class NumberComponent extends ValueComponent {
    private static final float HEIGHT = 25.0F;
    private boolean dragging;

    public NumberComponent(NumberValue value) {
        super(value);
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    @Override
    public float render(Canvas canvas, float mouseX, float mouseY, float x, float y, float width, float alpha) {
        NumberValue value = (NumberValue) getValue();
        if (dragging) updateValue(value, mouseX, x, width);

        if (hovered(mouseX, mouseY, x, y, width, HEIGHT)) {
            SkiaRender.roundRect(canvas, x, y, width, HEIGHT, 3.0F, SkiaRender.alpha(0xFFFFFFFF, 0.035F * alpha));
        }
        SkiaFont label = Fonts.regular(8.0F);
        SkiaFont number = Fonts.number(8.0F);
        String text = format(value.getValue());
        float valueX = x + width - 5.0F - number.getStringWidth(text);
        String labelText = trim(label, value.getName(), valueX - (x + 13.0F));
        label.drawStringCenteredY(canvas, labelText, x + 5.0F, y + 8.0F,
                SkiaRender.alpha(0xFFC8C8D0, alpha), false);
        number.drawStringCenteredY(canvas, text, valueX, y + 8.0F,
                SkiaRender.alpha(0xFFF2F2F5, alpha), false);

        float barX = x + 5.0F;
        float barY = y + 18.0F;
        float barWidth = Math.max(0.0F, width - 10.0F);
        float range = value.getMax() - value.getMin();
        float ratio = range <= 0.0F ? 0.0F : (value.getValue() - value.getMin()) / range;
        ratio = Math.max(0.0F, Math.min(1.0F, ratio));
        SkiaRender.roundRect(canvas, barX, barY, barWidth, 3.0F, 1.5F, SkiaRender.alpha(0xFF41414B, alpha));
        if (ratio > 0.0F) {
            SkiaRender.roundRect(canvas, barX, barY, Math.max(3.0F, barWidth * ratio), 3.0F, 1.5F,
                    SkiaRender.alpha(PanelRenderer.accent(0), alpha));
        }
        SkiaRender.circle(canvas, barX + barWidth * ratio, barY + 1.5F, 3.0F,
                SkiaRender.alpha(0xFFFFFFFF, alpha));
        return lastHeight = HEIGHT;
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) return Integer.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String trim(SkiaFont font, String text, float maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        String trimmed = text;
        while (!trimmed.isEmpty() && font.getStringWidth(trimmed + "…") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.offsetByCodePoints(trimmed.length(), -1));
        }
        return trimmed.isEmpty() ? "…" : trimmed + "…";
    }

    private static void updateValue(NumberValue value, float mouseX, float x, float width) {
        float usableWidth = Math.max(1.0F, width - 10.0F);
        float percent = Math.max(0.0F, Math.min(1.0F, (mouseX - x - 5.0F) / usableWidth));
        float raw = value.getMin() + (value.getMax() - value.getMin()) * percent;
        float increment = value.getInc();
        if (increment > 0.0F) raw = value.getMin() + Math.round((raw - value.getMin()) / increment) * increment;
        value.setValue(raw);
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button, float x, float y, float width) {
        if (button == 0 && hovered(mouseX, mouseY, x, y, width, HEIGHT)) {
            dragging = true;
            updateValue((NumberValue) getValue(), mouseX, x, width);
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (button == 0) dragging = false;
    }
}
