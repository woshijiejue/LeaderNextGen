package cn.leader.ui.clickgui.components.impl;

import cn.leader.module.value.impl.ModeValue;
import cn.leader.ui.clickgui.components.ValueComponent;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.SkiaRender;
import org.jetbrains.skija.Canvas;

public final class ModeComponent extends ValueComponent {
    private static final float HEIGHT = 18.0F;

    public ModeComponent(ModeValue value) {
        super(value);
    }

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    @Override
    public float render(Canvas canvas, float mouseX, float mouseY, float x, float y, float width, float alpha) {
        ModeValue value = (ModeValue) getValue();
        if (hovered(mouseX, mouseY, x, y, width, HEIGHT)) {
            SkiaRender.roundRect(canvas, x, y, width, HEIGHT, 3.0F, SkiaRender.alpha(0xFFFFFFFF, 0.035F * alpha));
        }

        SkiaFont label = Fonts.regular(6.5F);
        SkiaFont mode = Fonts.medium(7.5F);
        float right = x + width - 5.0F;
        String text = value.getValue();
        float arrowWidth = 8.0F;
        float valueRight = right - arrowWidth;
        text = trim(mode, text, width * 0.52F);
        float valueX = valueRight - mode.getStringWidth(text);
        String labelText = trim(label, value.getName(), Math.max(0.0F, valueX - (x + 10.0F)));
        label.drawStringCenteredY(canvas, labelText, x + 5.0F, y + HEIGHT / 2.0F,
                SkiaRender.alpha(0xFFA8A8B3, alpha), false);
        mode.drawStringCenteredY(canvas, text, valueX, y + HEIGHT / 2.0F,
                SkiaRender.alpha(0xFFE9E9EF, alpha), false);
        SkiaRender.line(canvas, right - 1.5F, y + HEIGHT / 2.0F - 2.0F, right, y + HEIGHT / 2.0F,
                1.0F, SkiaRender.alpha(0xFFAAAAAF, alpha));
        SkiaRender.line(canvas, right, y + HEIGHT / 2.0F, right - 1.5F, y + HEIGHT / 2.0F + 2.0F,
                1.0F, SkiaRender.alpha(0xFFAAAAAF, alpha));
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
        if ((button != 0 && button != 1) || !hovered(mouseX, mouseY, x, y, width, HEIGHT)) return;
        ModeValue mode = (ModeValue) getValue();
        String[] modes = mode.getModes();
        if (modes.length == 0) return;
        int current = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(mode.getValue())) {
                current = i;
                break;
            }
        }
        int delta = button == 0 ? -1 : 1;
        mode.setValue(modes[Math.floorMod(current + delta, modes.length)]);
    }
}
