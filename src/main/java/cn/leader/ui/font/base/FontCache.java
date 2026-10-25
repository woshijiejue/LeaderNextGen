package cn.leader.ui.font.base;

import net.minecraft.ChatFormatting;
import org.jetbrains.skija.TextBlob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FontCache {
    private final Map<CacheKey, TextRecord> cache;

    public FontCache(int maxCapacity) {
        this.cache = new LinkedHashMap<>(maxCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, TextRecord> eldest) {
                if (size() > maxCapacity) {
                    eldest.getValue().close();
                    return true;
                }
                return false;
            }
        };
    }

    public TextRecord get(String text, int color, SkiaFont font) {
        return cache.computeIfAbsent(new CacheKey(text, color), b -> new TextRecord(text, color, font));
    }

    public void clear() {
        cache.values().forEach(TextRecord::close);
        cache.clear();
    }

    public static class TextRecord {
        public final Segment[] segments;
        public final float width;

        public TextRecord(String text, int baseColor, SkiaFont font) {
            List<Segment> segmentList = new ArrayList<>();
            int color = baseColor;
            float width = 0;

            StringBuilder buffer = new StringBuilder();

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '§' && i + 1 < text.length()) {
                    if (!buffer.isEmpty()) {
                        width += flushBuffer(buffer, segmentList, color, font);
                    }

                    ChatFormatting format = ChatFormatting.getByCode(text.charAt(++i));
                    if (format != null) {
                        if (format == ChatFormatting.RESET) {
                            color = baseColor;
                        } else {
                            Integer formatColor = colorOf(format);
                            if (formatColor != null) {
                                color = (((baseColor >> 24) & 0xFF) << 24) | formatColor;
                            }
                        }
                    }
                } else {
                    buffer.append(c);
                }
            }

            if (!buffer.isEmpty()) {
                width += flushBuffer(buffer, segmentList, color, font);
            }

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

        private float flushBuffer(StringBuilder buffer, List<Segment> list, int color, SkiaFont font) {
            String str = buffer.toString();
            buffer.setLength(0);
            TextBlob blob = font.getShaper().shape(str, font.getFont());
            float width = font.getFont().measureTextWidth(str);
            list.add(new Segment(blob, color, width));
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

    private record CacheKey(String text, int color) {}
    public record Segment(TextBlob blob, int color, float width) {}
}
