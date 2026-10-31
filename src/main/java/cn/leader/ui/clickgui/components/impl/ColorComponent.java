package cn.leader.ui.clickgui.components.impl;

import cn.leader.module.value.impl.ColorValue;
import cn.leader.ui.clickgui.components.ValueComponent;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.SkiaRender;
import org.jetbrains.skija.Canvas;

import java.awt.Color;

public final class ColorComponent extends ValueComponent {
    private static final float HEADER_HEIGHT = 18.0F;
    private static final float PICKER_HEIGHT = 50.0F;
    private static final float HUE_HEIGHT = 8.0F;
    private boolean expanded;
    private boolean draggingSaturationBrightness;
    private boolean draggingHue;

    public ColorComponent(ColorValue value) {
        super(value);
    }

    @Override
    public float getHeight() {
        return HEADER_HEIGHT + (expanded ? PICKER_HEIGHT + HUE_HEIGHT + 8.0F : 0.0F);
    }

    @Override
    public float render(Canvas canvas, float mouseX, float mouseY, float x, float y, float width, float alpha) {
        ColorValue value = (ColorValue) getValue();
        if (draggingSaturationBrightness) updateSaturationBrightness(value, mouseX, mouseY, x, y, width);
        if (draggingHue) updateHue(value, mouseX, x, width);

        if (hovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) {
            SkiaRender.roundRect(canvas, x, y, width, HEADER_HEIGHT, 3.0F, SkiaRender.alpha(0xFFFFFFFF, 0.035F * alpha));
        }
        SkiaFont font = Fonts.regular(8.5F);
        font.drawStringCenteredY(canvas, value.getName(), x + 5.0F, y + HEADER_HEIGHT / 2.0F,
                SkiaRender.alpha(0xFFC8C8D0, alpha), false);
        SkiaRender.roundRect(canvas, x + width - 22.0F, y + 4.0F, 16.0F, 10.0F, 3.0F,
                SkiaRender.alpha(value.getValue().getRGB(), alpha));
        SkiaRender.roundRectOutline(canvas, x + width - 22.0F, y + 4.0F, 16.0F, 10.0F, 3.0F, 0.7F,
                SkiaRender.alpha(0xBFFFFFFF, alpha));

        if (expanded) drawPicker(canvas, value, x, y + HEADER_HEIGHT + 3.0F, width, alpha);
        lastHeight = getHeight();
        return lastHeight;
    }

    private void drawPicker(Canvas canvas, ColorValue value, float x, float y, float width, float alpha) {
        float swatchWidth = Math.max(1.0F, width - 10.0F);
        float swatchX = x + 5.0F;
        float hueY = y + PICKER_HEIGHT + 3.0F;
        int pureHue = Color.getHSBColor(value.getHue(), 1.0F, 1.0F).getRGB();
        SkiaRender.roundRect(canvas, swatchX, y, swatchWidth, PICKER_HEIGHT, 4.0F, SkiaRender.alpha(pureHue, alpha));
        SkiaRender.horizontalGradient(canvas, swatchX, y, swatchWidth, PICKER_HEIGHT, 4.0F,
                SkiaRender.alpha(0xFFFFFFFF, alpha), SkiaRender.alpha(pureHue, alpha));
        SkiaRender.verticalGradient(canvas, swatchX, y, swatchWidth, PICKER_HEIGHT, 4.0F,
                SkiaRender.alpha(0x00000000, alpha), SkiaRender.alpha(0xFF000000, alpha));

        float cursorX = swatchX + value.getSaturation() * swatchWidth;
        float cursorY = y + (1.0F - value.getBrightness()) * PICKER_HEIGHT;
        SkiaRender.circle(canvas, cursorX, cursorY, 3.0F, SkiaRender.alpha(0xFF101014, alpha));
        SkiaRender.circle(canvas, cursorX, cursorY, 2.0F, SkiaRender.alpha(0xFFFFFFFF, alpha));

        int[] hues = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
        float segmentWidth = swatchWidth / 6.0F;
        for (int i = 0; i < 6; i++) {
            SkiaRender.horizontalGradient(canvas, swatchX + i * segmentWidth, hueY, segmentWidth + 0.5F,
                    HUE_HEIGHT, 3.0F, SkiaRender.alpha(hues[i], alpha), SkiaRender.alpha(hues[i + 1], alpha));
        }
        float markerX = swatchX + value.getHue() * swatchWidth;
        SkiaRender.roundRect(canvas, markerX - 1.0F, hueY - 1.0F, 2.0F, HUE_HEIGHT + 2.0F, 1.0F,
                SkiaRender.alpha(0xFFFFFFFF, alpha));
    }

    private static void updateSaturationBrightness(ColorValue value, float mouseX, float mouseY,
                                                   float x, float y, float width) {
        float pickerX = x + 5.0F;
        float pickerY = y + HEADER_HEIGHT + 3.0F;
        float pickerWidth = Math.max(1.0F, width - 10.0F);
        float saturation = Math.max(0.0F, Math.min(1.0F, (mouseX - pickerX) / pickerWidth));
        float brightness = 1.0F - Math.max(0.0F, Math.min(1.0F, (mouseY - pickerY) / PICKER_HEIGHT));
        value.setHSB(value.getHue(), saturation, brightness);
    }

    private static void updateHue(ColorValue value, float mouseX, float x, float width) {
        float pickerWidth = Math.max(1.0F, width - 10.0F);
        float hue = Math.max(0.0F, Math.min(1.0F, (mouseX - x - 5.0F) / pickerWidth));
        value.setHSB(hue, value.getSaturation(), value.getBrightness());
    }

    @Override
    public void mouseClicked(float mouseX, float mouseY, int button, float x, float y, float width) {
        if (button == 1 && hovered(mouseX, mouseY, x, y, width, HEADER_HEIGHT)) {
            expanded = !expanded;
            return;
        }
        if (button != 0 || !expanded) return;

        float pickerY = y + HEADER_HEIGHT + 3.0F;
        if (hovered(mouseX, mouseY, x + 5.0F, pickerY, Math.max(1.0F, width - 10.0F), PICKER_HEIGHT)) {
            draggingSaturationBrightness = true;
            updateSaturationBrightness((ColorValue) getValue(), mouseX, mouseY, x, y, width);
        } else if (hovered(mouseX, mouseY, x + 5.0F, pickerY + PICKER_HEIGHT + 3.0F,
                Math.max(1.0F, width - 10.0F), HUE_HEIGHT)) {
            draggingHue = true;
            updateHue((ColorValue) getValue(), mouseX, x, width);
        }
    }

    @Override
    public void mouseReleased(float mouseX, float mouseY, int button) {
        if (button == 0) {
            draggingSaturationBrightness = false;
            draggingHue = false;
        }
    }
}
