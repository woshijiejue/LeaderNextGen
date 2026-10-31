package cn.leader.util.render;

import org.jetbrains.skija.*;

public final class SkiaRender {
    private static final Paint PAINT = new Paint();

    private static Paint fill(int color) {
        PAINT.reset();
        PAINT.setAntiAlias(true);
        PAINT.setColor(color);
        return PAINT;
    }

    private static Paint stroke(int color, float width) {
        Paint paint = fill(color);
        paint.setMode(PaintMode.STROKE);
        paint.setStrokeWidth(width);
        return paint;
    }

    public static int alpha(int color, float factor) {
        int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, factor)));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static void rect(Canvas canvas, float x, float y, float width, float height, int color) {
        canvas.drawRect(Rect.makeXYWH(x, y, width, height), fill(color));
    }

    public static void roundRect(Canvas canvas, float x, float y, float width, float height, float radius, int color) {
        canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), fill(color));
    }

    public static void roundRect(Canvas canvas, float x, float y, float width, float height, float tl, float tr, float br, float bl, int color) {
        canvas.drawRRect(RRect.makeXYWH(x, y, width, height, tl, tr, br, bl), fill(color));
    }

    public static void roundRectOutline(Canvas canvas, float x, float y, float width, float height, float radius, float lineWidth, int color) {
        canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), stroke(color, lineWidth));
    }

    public static void horizontalGradient(Canvas canvas, float x, float y, float width, float height, float radius, int left, int right) {
        try (Shader shader = Shader.makeLinearGradient(x, y, x + width, y, new int[]{left, right})) {
            Paint paint = fill(0xFFFFFFFF);
            paint.setShader(shader);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
            paint.setShader(null);
        }
    }

    public static void verticalGradient(Canvas canvas, float x, float y, float width, float height, float radius, int top, int bottom) {
        try (Shader shader = Shader.makeLinearGradient(x, y, x, y + height, new int[]{top, bottom})) {
            Paint paint = fill(0xFFFFFFFF);
            paint.setShader(shader);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
            paint.setShader(null);
        }
    }

    public static void gradientOutline(Canvas canvas, float x, float y, float width, float height, float radius, float lineWidth, int left, int right) {
        try (Shader shader = Shader.makeLinearGradient(x, y, x + width, y + height, new int[]{left, right})) {
            Paint paint = stroke(0xFFFFFFFF, lineWidth);
            paint.setShader(shader);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
            paint.setShader(null);
        }
    }

    public static void circle(Canvas canvas, float centerX, float centerY, float radius, int color) {
        canvas.drawCircle(centerX, centerY, radius, fill(color));
    }

    public static void line(Canvas canvas, float x1, float y1, float x2, float y2, float width, int color) {
        Paint paint = stroke(color, width);
        paint.setStrokeCap(PaintStrokeCap.ROUND);
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    public static void blurredRoundRect(Canvas canvas, float x, float y, float width, float height, float radius, float blur, int color) {
        if (blur <= 0.0F) {
            roundRect(canvas, x, y, width, height, radius, color);
            return;
        }

        try (MaskFilter filter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blur / 2.0F)) {
            Paint paint = fill(color);
            paint.setMaskFilter(filter);
            canvas.drawRRect(RRect.makeXYWH(x, y, width, height, radius), paint);
            paint.setMaskFilter(null);
        }
    }

    public static void clipRoundRect(Canvas canvas, float x, float y, float width, float height, float radius) {
        canvas.clipRRect(RRect.makeXYWH(x, y, width, height, radius), ClipMode.INTERSECT, true);
    }

    public static void excludeRoundRect(Canvas canvas, float x, float y, float width, float height, float radius) {
        canvas.clipRRect(RRect.makeXYWH(x, y, width, height, radius), ClipMode.DIFFERENCE, true);
    }

    private SkiaRender() {
    }
}
