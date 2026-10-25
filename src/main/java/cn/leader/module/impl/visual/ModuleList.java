package cn.leader.module.impl.visual;

import cn.leader.Client;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.ColorUtil;
import cn.leader.util.render.FontUtil;
import cn.leader.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.ChatFormatting;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ModuleList extends Drag {
    private final BoolValue important = new BoolValue("Important", false);
    private final BoolValue suffix = new BoolValue("Suffix", true);
    private final BoolValue background = new BoolValue("Background", true);

    public ModuleList() {
        super("ModuleList");
        setDescription("Module list");
        percentX = 0.95F;
        percentY = 0.05F;
        setEnabled(true);
    }

    @Override
    public boolean isRightAnchored() {
        return percentX > 0.5F;
    }

    @Override
    public void render(GuiGraphicsExtractor context) {
        if (mc.font == null) return;

        final HUD hud = getModule(HUD.class);
        final List<Module> modules = Client.instance.getModuleManager().getModuleMap().values().stream()
                .filter(m -> !m.isHidden() && (m.isEnabled() || m.getAnimation().getValue() > 0))
                .filter(m -> !important.getValue() || (m.getCategory() != Category.Client && m.getCategory() != Category.Visual))
                .sorted(Comparator.comparingInt((Module m) -> (int) FontUtil.getStringWidth(m.getName() + (suffix.getValue() && !m.getSuffix().isEmpty() ? " " + m.getSuffix() : ""))).reversed().thenComparing(Module::getName))
                .toList();

        if (modules.isEmpty()) return;

        float maxWidth = 0;
        for (final Module module : modules) {
            final String displayName = module.getName() + (suffix.getValue() && !module.getSuffix().isEmpty() ? " " + module.getSuffix() : "");
            maxWidth = Math.max(maxWidth, FontUtil.getStringWidth(displayName));
        }
        this.width = maxWidth + 4;

        float offsetY = renderY;
        int index = 0;

        for (final Module module : modules) {
            if (!Objects.equals(module.getName(), "HUD")) {
                module.getAnimation().run(module.isEnabled() ? 1 : 0);
                final float alpha = (float) module.getAnimation().getValue();
                if (alpha <= 0.02f) continue;

                final boolean hasSuffix = suffix.getValue() && !module.getSuffix().isEmpty();
                final String moduleName = module.getName();
                final String suffixName = hasSuffix ? " " + module.getSuffix() : "";

                final float textWidth = FontUtil.getStringWidth(moduleName + suffixName);
                final float offset = (1 - alpha) * (this.width + 10);
                final float x = isRightAnchored() ? (renderX - textWidth + offset) : (renderX - offset);

                if (background.getValue()) {
                    RenderUtil.drawRect(context, x - 2, offsetY, textWidth + 4, FontUtil.getHeight(), ColorUtil.applyAlpha(new Color(0, 0, 0, 120), alpha).getRGB());
                }

                FontUtil.drawStringWithShadow(context, moduleName + ChatFormatting.GRAY + suffixName, x, offsetY, ColorUtil.applyAlpha(hud.getColor(index++), alpha));
                offsetY += FontUtil.getHeight() * alpha;
            }
        }

        this.height = Math.max(FontUtil.getHeight(), offsetY - renderY);
    }
}