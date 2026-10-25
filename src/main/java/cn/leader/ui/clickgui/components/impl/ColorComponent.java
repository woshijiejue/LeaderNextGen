package cn.leader.ui.clickgui.components.impl;

import cn.leader.module.value.impl.ColorValue;
import cn.leader.ui.clickgui.components.ValueComponent;
import cn.leader.util.render.FontUtil;
import cn.leader.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import java.awt.Color;

public class ColorComponent extends ValueComponent {
    private boolean expanded, draggingSB, draggingHue;

    public ColorComponent(ColorValue value) {
        super(value);
    }

    @Override
    public float render(GuiGraphicsExtractor context, int mouseX, int mouseY, float x, float y, float width) {
        ColorValue color = (ColorValue) getValue();
        float height = 15;

        RenderUtil.drawRect(context, x, y, width, 15, new Color(0, 0, 0, 120).getRGB());
        FontUtil.drawStringWithShadow(context, color.getName(), x + 6, y + 4, 0xFFAAAAAA);
        RenderUtil.drawRect(context, x + width - 16, y + 3, 10, 9, color.getValue().getRGB());

        if (expanded) {
            float sbHeight = 50, hueHeight = 10;
            float pickerY = y + 15;

            if (draggingSB) {
                float s = Mth.clamp((mouseX - x) / width, 0f, 1f);
                float b = 1f - Mth.clamp((mouseY - pickerY) / sbHeight, 0f, 1f);
                color.setHSB(color.getHue(), s, b);
            }
            if (draggingHue) {
                float h = Mth.clamp((mouseX - x) / width, 0f, 1f);
                color.setHSB(h, color.getSaturation(), color.getBrightness());
            }

            int pureHue = Color.getHSBColor(color.getHue(), 1f, 1f).getRGB();
            RenderUtil.drawGradientRect(context, x, pickerY, width, sbHeight, 0xFFFFFFFF, pureHue, true);
            RenderUtil.drawGradientRect(context, x, pickerY, width, sbHeight, 0x00000000, 0xFF000000, false);

            float pX = x + color.getSaturation() * width;
            float pY = pickerY + (1f - color.getBrightness()) * sbHeight;
            RenderUtil.drawRect(context, pX - 1.5f, pY - 1.5f, 3, 3, 0xFFFFFFFF);

            float hueY = pickerY + sbHeight;
            float seg = width / 6f;
            int[] hues = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};

            for (int i = 0; i < 6; i++) {
                float segX = x + i * seg;
                float nextX = x + (i + 1) * seg;
                RenderUtil.drawGradientRect(context, segX, hueY, nextX - segX, hueHeight, hues[i], hues[i+1], true);
            }

            RenderUtil.drawRect(context, x + color.getHue() * width - 1, hueY, 2, hueHeight, 0xFFFFFFFF);
            height += sbHeight + hueHeight;
        }

        return lastHeight = height;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button, float x, float y, float width) {
        if (hovered(mouseX, mouseY, x, y, width, 15)) {
            if (button == 1) expanded = !expanded;
        } else if (expanded) {
            float pickerY = y + 15;
            if (button == 0) {
                if (hovered(mouseX, mouseY, x, pickerY, width, 50)) {
                    draggingSB = true;
                } else if (hovered(mouseX, mouseY, x, pickerY + 50, width, 10)) {
                    draggingHue = true;
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        draggingSB = draggingHue = false;
    }
}