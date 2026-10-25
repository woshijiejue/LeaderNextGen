package cn.leader.ui.font.base;

import org.jetbrains.skija.*;
import org.jetbrains.skija.shaper.Shaper;

/**
 * Skia Font
 * @author DSJ
 */
public class SkiaFont {
    private final FontCache cache = new FontCache(8964);
    private final Paint paint = new Paint().setAntiAlias(true);
    private final Shaper shaper = Shaper.make();
    private final Typeface typeface;
    private final Font font;

    private final float fontHeight;

    public SkiaFont(Typeface typeface, float size) {
        this.typeface = typeface;
        this.font = new Font(typeface, size).setSubpixel(true).setEdging(FontEdging.SUBPIXEL_ANTI_ALIAS);
        FontMetrics metrics = this.font.getMetrics();
        this.fontHeight = metrics.getDescent() - metrics.getAscent();
    }

    public float getStringWidth(String text) {
        return (text == null || text.isEmpty()) ? 0.0f : cache.get(text, -1, this).width;
    }

    public void drawString(Canvas stack, String text, float x, float y, int color) {
        renderText(stack, text, x, y, color, false);
    }

    public void drawShadowString(Canvas stack, String text, float x, float y, int color) {
        renderText(stack, text, x, y, color, true);
    }

    private void renderText(Canvas canvas, String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        int alpha = (color >> 24) & 0xFF;
        if (alpha == 0 && (color & 0xFFFFFF) != 0) alpha = 255;
        if (alpha <= 1) return;
        canvas.save();
        if (shadow) drawBlobs(canvas, cache.get(text, color, this), x + 0.5f, y + 0.5f, ((int) (alpha * 0.6f) << 24), true);
        drawBlobs(canvas, cache.get(text, color, this), x, y, alpha, false);
        canvas.restore();
    }

    private void drawBlobs(Canvas canvas, FontCache.TextRecord record, float x, float y, int modifier, boolean shadow) {
        float offsetX = x;
        if (shadow) paint.setColor(modifier);

        for (FontCache.Segment seg : record.segments) {
            if (!shadow) paint.setColor((modifier << 24) | (seg.color() & 0xFFFFFF));
            canvas.drawTextBlob(seg.blob(), offsetX, y, paint);
            offsetX += seg.width();
        }
    }

    public void close() {
        cache.clear();
        font.close();
        typeface.close();
        shaper.close();
        paint.close();
    }

    public Typeface getTypeface() {
        return this.typeface;
    }

    public Font getFont() {
        return this.font;
    }

    public float getFontHeight() {
        return this.fontHeight;
    }

        public float getOffsetX() {
            return this.offsetX;
        }

}