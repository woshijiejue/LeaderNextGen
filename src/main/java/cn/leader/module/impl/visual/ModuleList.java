package cn.leader.module.impl.visual;

import cn.leader.Client;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import org.jetbrains.skija.Canvas;

import java.util.Comparator;
import java.util.List;

public final class ModuleList extends Drag {
    private final BoolValue important = new BoolValue("Important", false);
    private final BoolValue suffix = new BoolValue("Suffix", true);
    private final BoolValue lowercase = new BoolValue("Lowercase", false);
    private final BoolValue background = new BoolValue("Background", true);
    private final ModeValue bar = new ModeValue("Bar", "Side", "None", "Side", "Glow");
    private final NumberValue fontSize = new NumberValue("Font Size", 9, 7, 12, 0.5F);

    public ModuleList() {
        super("ModuleList");
        setDescription("Module list");
        percentX = 0.99F;
        percentY = 0.01F;
        setEnabled(true);
    }

    @Override
    public boolean isRightAnchored() {
        return percentX > 0.5F;
    }

    private String nameOf(Module module) {
        String name = module.getName();
        return lowercase.getValue() ? name.toLowerCase(java.util.Locale.ROOT) : name;
    }

    private String suffixOf(Module module) {
        if (!suffix.getValue() || module.getSuffix().isEmpty()) return "";
        String text = module.getSuffix();
        return lowercase.getValue() ? text.toLowerCase(java.util.Locale.ROOT) : text;
    }

    @Override
    public void render(Canvas canvas) {
        SkiaFont nameFont = Fonts.medium(fontSize.getValue());
        SkiaFont suffixFont = Fonts.regular(fontSize.getValue());
        boolean shadow = Fonts.shadow();
        HUD hud = instance.getModuleManager().getModule(HUD.class);
        float rowSpacing = hud == null ? 1.5F : hud.rowSpacing.getValue();

        final List<Module> modules = Client.instance.getModuleManager().getModuleMap().values().stream()
                .filter(m -> !m.isHidden() && !(m instanceof HUD) && (m.isEnabled() || m.getAnimation().getValue() > 0.02))
                .filter(m -> !important.getValue() || (m.getCategory() != Category.Client && m.getCategory() != Category.Visual))
                                .sorted(Comparator.comparingDouble((Module m) -> widthOf(m, nameFont, suffixFont)).reversed().thenComparing(Module::getName))
                .toList();

        float rowHeight = nameFont.getFontHeight() + 5.0F;
        float gap = rowSpacing;
        float radius = Math.min(PanelRenderer.radius(), rowHeight / 2.0F);
        boolean right = isRightAnchored();

        float maxWidth = 0;
        for (Module module : modules) {
            maxWidth = Math.max(maxWidth, widthOf(module, nameFont, suffixFont) + 10.0F);
        }
        this.width = maxWidth;

        // Zero spacing is still a background layout, but each row owns its own
        // surface.  Never create one background rectangle for the whole list:
        // that turns the ArrayList into a large black block.
        boolean compactLayout = gap <= 0.01F;

        float offsetY = renderY;
        float contentBottom = renderY;
        int index = 0;

        for (Module module : modules) {
            module.getAnimation().run(module.isEnabled() ? 1 : 0);
            float progress = (float) module.getAnimation().getValue();
            if (progress <= 0.02F) continue;

            String name = nameOf(module);
            String extra = suffixOf(module);
            float textWidth = widthOf(module, nameFont, suffixFont);
            float boxWidth = textWidth + 10.0F;
            float slide = (1 - progress) * (boxWidth + 12);
            float x = right ? renderX - boxWidth + slide : renderX - slide;
            float y = offsetY;
            contentBottom = Math.max(contentBottom, y + rowHeight);
            int color = PanelRenderer.accent(index * 2);

            if (background.getValue()) {
                // In compact mode the rows touch, but remain separate panels.
                // Keep post-processing enabled for normal spacing and use the
                // lightweight panel path for zero spacing to avoid shadow buildup.
                PanelRenderer.panel(canvas, x, y, boxWidth, rowHeight, radius,
                        progress, color, !compactLayout);
            } else if (!compactLayout) {
                SkiaRender.roundRect(canvas, x, y, boxWidth, rowHeight, radius,
                        SkiaRender.alpha(0xFF101014, 0.22F * progress));
            }

            if (!bar.is("None")) {
                float barX = right ? x + boxWidth - 2.5F : x + 0.5F;
                SkiaRender.roundRect(canvas, barX, y + 3.0F, 2.0F, rowHeight - 6.0F, 1.0F, SkiaRender.alpha(color, progress));
                if (bar.is("Glow")) {
                    SkiaRender.blurredRoundRect(canvas, barX - 1, y + 3.0F, 4.0F, rowHeight - 6.0F, 2.0F, 5.0F, SkiaRender.alpha(color, progress * 0.7F));
                }
            }

            float textX = x + (bar.is("None") ? 6.0F : (right ? 6.0F : 8.0F));
            float centerY = y + rowHeight / 2.0F;
            nameFont.drawStringCenteredY(canvas, name, textX, centerY, SkiaRender.alpha(color, progress), shadow);
            if (!extra.isEmpty()) {
                suffixFont.drawStringCenteredY(canvas, extra, textX + nameFont.getStringWidth(name) + 3.0F, centerY,
                        SkiaRender.alpha(0xFFA8A8B3, progress), shadow);
            }

            offsetY += (rowHeight + gap) * progress;
            index++;
        }

        this.height = Math.max(0.0F, contentBottom - renderY);
    }

    private float widthOf(Module module, SkiaFont nameFont, SkiaFont suffixFont) {
        String extra = suffixOf(module);
        float width = nameFont.getStringWidth(nameOf(module));
        if (!extra.isEmpty()) width += 3.0F + suffixFont.getStringWidth(extra);
        return width;
    }
}
