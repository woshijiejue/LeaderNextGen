package cn.leader.ui.skia;

import org.jetbrains.skija.*;

public final class Backdrop {
    private Surface blurSurface;
    private Image blurred;
    private int surfaceWidth = -1;
    private int surfaceHeight = -1;

    public void capture(Skia skia, int downsample, float radius) {
        release();
        if (skia.surface == null || skia.context == null) return;

        int width = Math.max(1, skia.getWidth() / downsample);
        int height = Math.max(1, skia.getHeight() / downsample);

        if (blurSurface == null || width != surfaceWidth || height != surfaceHeight) {
            if (blurSurface != null) blurSurface.close();
            blurSurface = Surface.makeRenderTarget(skia.context, false, ImageInfo.makeN32Premul(width, height));
            surfaceWidth = width;
            surfaceHeight = height;
        }

        if (blurSurface == null) return;

        try (Image frame = skia.surface.makeImageSnapshot();
             Paint paint = new Paint();
             ImageFilter filter = ImageFilter.makeBlur(radius / downsample, radius / downsample, FilterTileMode.CLAMP)) {
            paint.setImageFilter(filter);
            Canvas canvas = blurSurface.getCanvas();
            canvas.clear(0xFF000000);
            canvas.drawImageRect(frame, Rect.makeWH(frame.getWidth(), frame.getHeight()), Rect.makeWH(width, height), SamplingMode.LINEAR, paint, true);
        }

        blurred = blurSurface.makeImageSnapshot();
    }

    public boolean isReady() {
        return blurred != null;
    }

    public void draw(Canvas canvas, float x, float y, float width, float height, float radius, float screenWidth, float screenHeight, float alpha) {
        if (blurred == null) return;

        canvas.save();
        canvas.clipRRect(RRect.makeXYWH(x, y, width, height, radius), ClipMode.INTERSECT, true);
        try (Paint paint = new Paint()) {
            paint.setAlphaf(Math.max(0.0F, Math.min(1.0F, alpha)));
            canvas.drawImageRect(blurred, Rect.makeWH(blurred.getWidth(), blurred.getHeight()), Rect.makeWH(screenWidth, screenHeight), SamplingMode.LINEAR, paint, true);
        }
        canvas.restore();
    }

    public void release() {
        if (blurred != null) {
            blurred.close();
            blurred = null;
        }
    }

    public void destroy() {
        release();
        if (blurSurface != null) {
            blurSurface.close();
            blurSurface = null;
        }
    }
}
