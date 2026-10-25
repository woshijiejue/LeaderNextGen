package cn.leader.util.render;

import cn.leader.util.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@UtilityClass
public final class FontUtil implements IMinecraft {

    public void drawString(GuiGraphicsExtractor graphics, String text, float x, float y, int color, boolean shadow) {
        if (mc.font == null) return;

        graphics.text(mc.font, text, Math.round(x), Math.round(y), color, shadow);
    }

    public void drawStringWithShadow(GuiGraphicsExtractor graphics, String text, float x, float y, int color) {
        drawString(graphics, text, x, y, color, true);
    }

    public float getStringWidth(final String text) {
        return mc.font.width(text);
    }

    public float getHeight() {
        return mc.font.lineHeight;
    }
}
