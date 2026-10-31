package cn.leader.ui.clickgui.components.impl;

import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.MultiBoolValue;
import cn.leader.ui.clickgui.components.ValueComponent;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import org.jetbrains.skija.Canvas;

public final class MultiBoolComponent extends ValueComponent {
    private static final float HEADER_HEIGHT = 18.0F;
    private static final float OPTION_HEIGHT = 16.0F;
    private boolean expanded;

    public MultiBoolComponent(MultiBoolValue value) {
        super(value);
    }

    @Override
    public float getHeight() {
        MultiBoolValue multi = (MultiBoolValue) getValue();
        return HEADER_HEIGHT + (expanded ? OPTION_HEIGHT * multi.getValues().size() : 0.0F);
    }

    @Override
    public float render(Canvas canvas, float mouseX, float mouseY, float x, float y, float width, float alpha) {
        MultiBoolValue multi = (MultiBoolValue) getValue();
        SkiaFont font = Fonts.regular(8.0F);
        boolean hovered = hovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT);
        if (hovered) SkiaRender.roundRect(canvas, x, y, width, HEADER_HEIGHT, 3.0F,
                SkiaRender.alpha(0xFFFFFFFF, 0.035F * alpha));

        font.drawStringCenteredY(canvas, multi.getName(), x + 5.0F, y + HEADER_HEIGHT / 2.0F,
                SkiaRender.alpha(0xFFC8C8D0, alpha), false);
        String indicator = expanded ? "−" : "+";
        font.drawStringCenteredY(canvas, indicator, x + width - 10.0F, y + HEADER_HEIGHT / 2.0F,
                SkiaRender.alpha(0xFFA8A8B3, alpha), false);

        float offset = HEADER_HEIGHT;
        if (expanded) {
            for (BoolValue value : multi.getValues()) {
                float rowY = y + offset;
                SkiaRender.roundRect(canvas, x + 3.0F, rowY + 1.0F, width - 6.0F, OPTION_HEIGHT - 2.0F,
                        3.0F, SkiaRender.alpha(0xFFFFFFFF, hovered(mouseX, mouseY, x, rowY, width, OPTION_HEIGHT) ? 0.04F * alpha : 0.015F * alpha));
                float boxX = x + 7.0F;
                float boxY = rowY + 4.0F;
                int accent = PanelRenderer.accent(0);
                SkiaRender.roundRect(canvas, boxX, boxY, 8.0F, 8.0F, 2.0F,
                        SkiaRender.alpha(value.getValue() ? accent : 0xFF555560, alpha));
                if (value.getValue()) {
                    SkiaRender.line(canvas, boxX + 2.0F, boxY + 4.0F, boxX + 3.5F, boxY + 5.5F, 1.0F,
                            SkiaRender.alpha(0xFFFFFFFF, alpha));
                    SkiaRender.line(canvas, boxX + 3.5F, boxY + 5.5F, boxX + 6.5F, boxY + 2.5F, 1.0F,
                            SkiaRender.alpha(0xFFFFFFFF, alpha));
                }
                font.drawStringCenteredY(canvas, value.getName(), x + 20.0F, rowY + OPTION_HEIGHT / 2.0F,
                        SkiaRender.alpha(value.getValue() ? 0xFFE9E9EF : 0xFF9A9AA6, alpha), false);
                offset += OPTION_HEIGHT;
            }
        }
        lastHeight = getHeight();
        return lastHeight;
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button, float x, float y, float width) {
        if (hovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) {
            if (button == 1) expanded = !expanded;
            return;
        }
        if (!expanded || button != 0) return;

        MultiBoolValue multi = (MultiBoolValue) getValue();
        float optionY = y + HEADER_HEIGHT;
        for (BoolValue value : multi.getValues()) {
            if (hovered(mouseX, mouseY, x, optionY, width, OPTION_HEIGHT)) {
                value.toggle();
                return;
            }
            optionY += OPTION_HEIGHT;
        }
    }
}
