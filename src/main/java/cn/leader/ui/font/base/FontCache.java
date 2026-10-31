package cn.leader.ui.font.base;

import net.minecraft.ChatFormatting;
import org.jetbrains.skija.Font;
import org.jetbrains.skija.TextBlob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FontCache {
    public static final int BASE_COLOR = -1;

    private final Map<String, TextRecord> cache;

    public FontCache(int maxCapacity) {
        this.cache = new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TextRecord> eldest) {
                if (size() > maxCapacity) {
                    eldest.getValue().close();
                    return true;
                }
                return false;
            }
        };
    }

    public TextRecord get(String text, SkiaFont font) {
        return cache.computeIfAbsent(text, key -> new TextRecord(key, font));
    }

    public void clear() {
        cache.values().forEach(TextRecord::close);
        cache.clear();
    }

    public static class TextRecord {
        public final Segment[] segments;
        public final float width;

        public TextRecord(String text, SkiaFont font) {
            List<Segment> segmentList = new ArrayList<>();
            int color = BASE_COLOR;
            float width = 0;

            StringBuilder buffer = new StringBuilder();
            boolean bufferFallback = false;

            for (int i = 0; i < text.length(); ) {
                int codePoint = text.codePointAt(i);
                int length = Character.charCount(codePoint);

                if (codePoint == '\u00a7' && i + 1 < text.length()) {
                    width += flush(buffer, segmentList, color, bufferFallback, font);
                    ChatFormatting format = ChatFormatting.getByCode(text.charAt(i + 1));
                    if (format != null) {
                        if (format == ChatFormatting.RESET) {
                            color = BASE_COLOR;
                        } else {
                            Integer formatColor = colorOf(format);
                            if (formatColor != null) {
                                color = formatColor;
                            }
                        }
                    }
                    i += 2;
                    continue;
                }

                boolean needsFallback = font.getFallback() != null
                        && !Character.isWhitespace(codePoint)
                        && font.getFont().getUTF32Glyph(codePoint) == 0;

                if (!buffer.isEmpty() && needsFallback != bufferFallback) {
                    width += flush(buffer, segmentList, color, bufferFallback, font);
                }

                bufferFallback = needsFallback;
                buffer.appendCodePoint(codePoint);
                i += length;
            }

            width += flush(buffer, segmentList, color, bufferFallback, font);

            this.segments = segmentList.toArray(new Segment[0]);
            this.width = width;
        }

        private static Integer colorOf(ChatFormatting format) {
            return switch (format) {
                case BLACK -> 0x000000;
                case DARK_BLUE -> 0x0000AA;
                case DARK_GREEN -> 0x00AA00;
                case DARK_AQUA -> 0x00AAAA;
                case DARK_RED -> 0xAA0000;
                case DARK_PURPLE -> 0xAA00AA;
                case GOLD -> 0xFFAA00;
                case GRAY -> 0xAAAAAA;
                case DARK_GRAY -> 0x555555;
                case BLUE -> 0x5555FF;
                case GREEN -> 0x55FF55;
                case AQUA -> 0x55FFFF;
                case RED -> 0xFF5555;
                case LIGHT_PURPLE -> 0xFF55FF;
                case YELLOW -> 0xFFFF55;
                case WHITE -> 0xFFFFFF;
                default -> null;
            };
        }

        private static float flush(StringBuilder buffer, List<Segment> list, int color, boolean fallback, SkiaFont font) {
            if (buffer.isEmpty()) return 0;
            String str = buffer.toString();
            buffer.setLength(0);
            Font target = fallback ? font.getFallback() : font.getFont();
            TextBlob blob = font.getShaper().shape(str, target);
            float width = target.measureTextWidth(str);
            Float firstBaseline = blob.getFirstBaseline();
            float blobBaseline = firstBaseline == null ? 0.0F : firstBaseline;
            list.add(new Segment(blob, color, width, fallback, blobBaseline));
            return width;
        }

        public void close() {
            for (Segment seg : segments) {
                if (seg.blob() != null) {
                    seg.blob().close();
                }
            }
        }
    }

    public record Segment(TextBlob blob, int color, float width, boolean fallback, float blobBaseline) {}
}
