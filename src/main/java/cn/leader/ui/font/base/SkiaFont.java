package cn.leader.ui.font.base;

import org.jetbrains.skija.*;
import org.jetbrains.skija.shaper.Shaper;

public class SkiaFont {
    private final FontCache cache = new FontCache(4096);
    private final Paint paint = new Paint().setAntiAlias(true);
    private final Shaper shaper = Shaper.make();
    private final Font font;
    private final Font fallback;

    private final float fontHeight;
    private final float ascent;
    private final float fallbackShift;
    private final float fallbackAscent;
    private final float fallbackDescent;
    private final float primaryDescent;

    public SkiaFont(Typeface typeface, Typeface fallbackTypeface, float size) {
        this.font = new Font(typeface, size).setSubpixel(true).setEdging(FontEdging.ANTI_ALIAS);
        this.fallback = fallbackTypeface == null ? null : new Font(fallbackTypeface, size).setSubpixel(true).setEdging(FontEdging.ANTI_ALIAS);

        FontMetrics metrics = this.font.getMetrics();
        this.fontHeight = metrics.getDescent() - metrics.getAscent();
        this.ascent = -metrics.getAscent();
        this.primaryDescent = metrics.getDescent();

        if (this.fallback == null) {
            this.fallbackShift = 0.0F;
            this.fallbackAscent = this.ascent;
            this.fallbackDescent = this.primaryDescent;
        } else {
            FontMetrics fallbackMetrics = this.fallback.getMetrics();
            this.fallbackAscent = -fallbackMetrics.getAscent();
            this.fallbackDescent = fallbackMetrics.getDescent();

            this.fallbackShift = ((this.fallbackAscent - this.fallbackDescent)
                    - (this.ascent - this.primaryDescent)) * 0.5F;
        }
    }

    public float getStringWidth(String text) {
        return (text == null || text.isEmpty()) ? 0.0f : cache.get(text, this).width;
    }

    public void drawString(Canvas canvas, String text, float x, float y, int color) {
        renderText(canvas, text, x, y, color, false);
    }

    public void drawShadowString(Canvas canvas, String text, float x, float y, int color) {
        renderText(canvas, text, x, y, color, true);
    }

    public void drawString(Canvas canvas, String text, float x, float y, int color, boolean shadow) {
        renderText(canvas, text, x, y, color, shadow);
    }

    public void drawCenteredString(Canvas canvas, String text, float centerX, float y, int color, boolean shadow) {
        renderText(canvas, text, centerX - getStringWidth(text) / 2.0F, y, color, shadow);
    }

    public void drawStringCenteredY(Canvas canvas, String text, float x, float centerY, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        float fallbackTop = -fallbackAscent + fallbackShift;
        float fallbackBottom = fallbackDescent + fallbackShift;
        float top = fallback == null ? -ascent : Math.min(-ascent, fallbackTop);
        float bottom = fallback == null ? primaryDescent : Math.max(primaryDescent, fallbackBottom);
        float baseline = centerY - (top + bottom) * 0.5F;
        renderText(canvas, text, x, baseline, color, shadow);
    }

    private void renderText(Canvas canvas, String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        int alpha = (color >>> 24) & 0xFF;
        if (alpha == 0 && (color & 0xFFFFFF) != 0) alpha = 255;
        if (alpha <= 3) return;

        FontCache.TextRecord record = cache.get(text, this);
        if (shadow) {
            drawBlobs(canvas, record, x + 0.5F, y + 0.5F, color, (int) (alpha * 0.45F), true);
        }
        drawBlobs(canvas, record, x, y, color, alpha, false);
    }

    private void drawBlobs(Canvas canvas, FontCache.TextRecord record, float x, float y, int baseColor, int alpha, boolean shadow) {
        float offsetX = x;
        float baseline = y;

        for (FontCache.Segment seg : record.segments) {
            if (seg.blob() != null) {
                int rgb = seg.color() == FontCache.BASE_COLOR ? baseColor & 0xFFFFFF : seg.color();
                paint.setColor(shadow ? (alpha << 24) : (alpha << 24) | rgb);

                canvas.drawTextBlob(seg.blob(), offsetX,
                        baseline - seg.blobBaseline() + (seg.fallback() ? fallbackShift : 0), paint);
            }
            offsetX += seg.width();
        }
    }

    public void close() {
        cache.clear();
        font.close();
        if (fallback != null) fallback.close();
        shaper.close();
        paint.close();
    }

    public Font getFont() {
        return this.font;
    }

    public Font getFallback() {
        return this.fallback;
    }

    public float getFontHeight() {
        return this.fontHeight;
    }

    public float getAscent() {
        return this.ascent;
    }

    public float getDescent() {
        return this.primaryDescent;
    }

    public float getFallbackShift() {
        return this.fallbackShift;
    }

    public Shaper getShaper() {
        return this.shaper;
    }
}
