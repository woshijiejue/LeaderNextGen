package cn.leader.ui.font;

import cn.leader.ui.font.base.SkiaFont;
import org.jetbrains.skija.Data;
import org.jetbrains.skija.Typeface;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FontManager {
    private final Map<String, SkiaFont> cache = new ConcurrentHashMap<>();

    public SkiaFont getFont(int size) {
        return getFont("MiSans-Normal.ttf", size);
    }

    public SkiaFont getMediumFont(int size) {
        return getFont("MiSans-Medium.ttf", size);
    }

    public SkiaFont getSemiboldFont(int size) {
        return getFont("MiSans-Semibold.ttf", size);
    }

    public SkiaFont getBoldFont(int size) {
        return getFont("MiSans-Bold.ttf", size);
    }

    private SkiaFont getFont(String name, int size) {
        return cache.computeIfAbsent(name + ":" + size, k -> create(name, size));
    }

    public SkiaFont create(String name, int size) {
        try (InputStream is = SkiaFont.class.getResourceAsStream("/assets/clientbase/fonts/" + name)) {
            if (is == null) {
                throw new RuntimeException("Font resources not found: " + name);
            }

            try (Data fontData = Data.makeFromBytes(is.readAllBytes())) {
                return new SkiaFont(Typeface.makeFromData(fontData), size);
            }
        } catch (Exception ignored) {
            return new SkiaFont(Typeface.makeDefault(), size);
        }
    }

    public void destroy() {
        cache.values().forEach(SkiaFont::close);
        cache.clear();
    }
}