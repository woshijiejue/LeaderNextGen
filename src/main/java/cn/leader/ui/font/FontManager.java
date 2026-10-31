package cn.leader.ui.font;

import cn.leader.ui.font.base.SkiaFont;
import org.jetbrains.skija.Data;
import org.jetbrains.skija.Typeface;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FontManager {
    public enum Family {
        POPPINS("Poppins", "Poppins-Regular.ttf", "Poppins-Medium.ttf", "Poppins-SemiBold.ttf", "Poppins-Bold.ttf"),
        MISANS("MiSans", "MiSans-Normal.ttf", "MiSans-Medium.ttf", "MiSans-Semibold.ttf", "MiSans-Bold.ttf"),
        JETBRAINS_MONO("JetBrains Mono", "JetBrainsMono-Regular.ttf", "JetBrainsMono-Regular.ttf", "JetBrainsMono-Bold.ttf", "JetBrainsMono-Bold.ttf");

        private final String displayName;
        private final String[] files;

        Family(String displayName, String... files) {
            this.displayName = displayName;
            this.files = files;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static Family byName(String name) {
            for (Family family : values()) {
                if (family.displayName.equalsIgnoreCase(name)) return family;
            }
            return POPPINS;
        }
    }

    public enum Weight {
        REGULAR, MEDIUM, SEMIBOLD, BOLD
    }

    private final Map<String, Typeface> typefaces = new ConcurrentHashMap<>();
    private final Map<String, SkiaFont> cache = new ConcurrentHashMap<>();

    public SkiaFont get(Family family, Weight weight, float size) {
        String key = family.name() + ":" + weight.name() + ":" + size;
        return cache.computeIfAbsent(key, k -> {
            Typeface primary = typeface(family.files[weight.ordinal()]);
            Typeface fallback = family == Family.MISANS ? null : typeface(Family.MISANS.files[weight.ordinal()]);
            return new SkiaFont(primary, fallback, size);
        });
    }

    public SkiaFont getFont(int size) {
        return get(Family.MISANS, Weight.REGULAR, size);
    }

    public SkiaFont getMediumFont(int size) {
        return get(Family.MISANS, Weight.MEDIUM, size);
    }

    public SkiaFont getSemiboldFont(int size) {
        return get(Family.MISANS, Weight.SEMIBOLD, size);
    }

    public SkiaFont getBoldFont(int size) {
        return get(Family.MISANS, Weight.BOLD, size);
    }

    private Typeface typeface(String file) {
        return typefaces.computeIfAbsent(file, FontManager::load);
    }

    private static Typeface load(String file) {
        try (InputStream is = SkiaFont.class.getResourceAsStream("/assets/leadernextgen/fonts/" + file)) {
            if (is == null) {
                throw new IllegalStateException("Font resource not found: " + file);
            }

            try (Data fontData = Data.makeFromBytes(is.readAllBytes())) {
                return Typeface.makeFromData(fontData);
            }
        } catch (Exception ignored) {
            return Typeface.makeDefault();
        }
    }

    public void destroy() {
        cache.values().forEach(SkiaFont::close);
        cache.clear();
        typefaces.values().forEach(Typeface::close);
        typefaces.clear();
    }
}
