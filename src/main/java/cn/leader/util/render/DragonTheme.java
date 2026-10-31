package cn.leader.util.render;

import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.Paint;
import org.jetbrains.skija.Rect;
import org.jetbrains.skija.SamplingMode;

import java.io.IOException;
import java.io.InputStream;

/** Small original UI accents for the white-and-gold dragon theme. */
public final class DragonTheme {
    public static final int WHITE = 0xFFF2F4F7;
    public static final int GOLD = 0xFFD5B56D;

    private static final String PORTRAIT_RESOURCE = "/assets/clientbase/images/gpt-white-dragon-portrait.png";
    private static Image portraitImage;

    public static void drawCrest(Canvas canvas, float x, float y, float size, float alpha) {
        float stroke = Math.max(0.9F, size * 0.075F);
        int white = SkiaRender.alpha(WHITE, alpha);
        int gold = SkiaRender.alpha(GOLD, alpha);

        // A compact, geometric horn-and-scale mark drawn from primitives.
        SkiaRender.line(canvas, x + size * 0.34F, y + size * 0.57F,
                x + size * 0.17F, y + size * 0.12F, stroke, gold);
        SkiaRender.line(canvas, x + size * 0.17F, y + size * 0.12F,
                x + size * 0.43F, y + size * 0.34F, stroke, gold);
        SkiaRender.line(canvas, x + size * 0.66F, y + size * 0.57F,
                x + size * 0.83F, y + size * 0.12F, stroke, gold);
        SkiaRender.line(canvas, x + size * 0.83F, y + size * 0.12F,
                x + size * 0.57F, y + size * 0.34F, stroke, gold);

        SkiaRender.line(canvas, x + size * 0.31F, y + size * 0.43F,
                x + size * 0.50F, y + size * 0.70F, stroke, white);
        SkiaRender.line(canvas, x + size * 0.69F, y + size * 0.43F,
                x + size * 0.50F, y + size * 0.70F, stroke, white);
        SkiaRender.circle(canvas, x + size * 0.40F, y + size * 0.46F, size * 0.045F, gold);
        SkiaRender.circle(canvas, x + size * 0.60F, y + size * 0.46F, size * 0.045F, gold);
    }

    public static void drawPortrait(Canvas canvas, float x, float y, float width, float height, float alpha) {
        Image image = portrait();
        if (image == null || width <= 0 || height <= 0) return;

        canvas.save();
        SkiaRender.clipRoundRect(canvas, x, y, width, height, 0.0F);
        try (Paint paint = new Paint()) {
            paint.setAlphaf(Math.max(0.0F, Math.min(1.0F, alpha)));
            paint.setBlendMode(org.jetbrains.skija.BlendMode.SRC_OVER);
            canvas.drawImageRect(image,
                    Rect.makeWH(image.getWidth(), image.getHeight()),
                    Rect.makeXYWH(x, y, width, height),
                    SamplingMode.LINEAR, paint, true);
        }
        canvas.restore();
    }

    private static Image portrait() {
        if (portraitImage != null) return portraitImage;
        try (InputStream stream = DragonTheme.class.getResourceAsStream(PORTRAIT_RESOURCE)) {
            if (stream == null) return null;
            portraitImage = Image.makeFromEncoded(stream.readAllBytes());
        } catch (IOException | RuntimeException ignored) {
            portraitImage = null;
        }
        return portraitImage;
    }

    public static void close() {
        if (portraitImage != null) {
            portraitImage.close();
            portraitImage = null;
        }
    }

    private DragonTheme() {
    }
}
