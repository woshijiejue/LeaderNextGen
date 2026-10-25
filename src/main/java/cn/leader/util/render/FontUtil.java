package cn.leader.util.render;

import cn.leader.util.IMinecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class FontUtil implements IMinecraft {

    public static void drawString(GuiGraphicsExtractor graphics, String text, float x, float y, int color, boolean shadow) {
        if (mc.font == null) return;

        graphics.text(mc.font, text, Math.round(x), Math.round(y), color, shadow);
    }

    public static void drawStringWithShadow(GuiGraphicsExtractor graphics, String text, float x, float y, int color) {
        drawString(graphics, text, x, y, color, true);
    }

    public static float getStringWidth(final String text) {
        return mc.font.width(text);
    }

    public static float getHeight() {
        return mc.font.lineHeight;
    }

    private FontUtil() {
    }
}
