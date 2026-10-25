package cn.leader.util.render;

import cn.leader.util.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.DrawContext;

@UtilityClass
public final class FontUtil implements IMinecraft {

    public void drawString(DrawContext context, String text, float x, float y, int color, boolean shadow) {
        if (mc.textRenderer == null) return;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.drawText(mc.textRenderer, text, 0, 0, color, shadow);
        context.getMatrices().pop();

    }

    public void drawStringWithShadow(DrawContext context, String text, float x, float y, int color) {
        drawString(context, text, x, y, color, true);
    }

    public float getStringWidth(final String text) {
        return mc.textRenderer.getWidth(text);
    }

    public float getHeight() {
        return mc.textRenderer.fontHeight;
    }
}