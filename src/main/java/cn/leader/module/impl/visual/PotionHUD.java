package cn.leader.module.impl.visual;

import cn.leader.module.value.impl.BoolValue;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.FontUtil;
import cn.leader.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Active status effects with remaining time.
 */
public final class PotionHUD extends Drag {

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private final BoolValue showDuration = new BoolValue("Duration", true);
    private final BoolValue background = new BoolValue("Background", true);
    private final BoolValue sortByDuration = new BoolValue("Sort By Duration", true);

    public PotionHUD() {
        super("PotionHUD");
        setDescription("Active status effects");
        percentX = 0.02F;
        percentY = 0.25F;
    }

    @Override
    public void render(GuiGraphicsExtractor context) {
        if (mc.player == null || mc.font == null) {
            this.width = 0;
            this.height = 0;
            return;
        }

        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        if (effects.isEmpty()) {
            this.width = 0;
            this.height = 0;
            return;
        }

        if (sortByDuration.getValue()) {
            effects.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed());
        }

        List<String> lines = new ArrayList<>();
        for (MobEffectInstance effect : effects) {
            String name = Component.translatable(effect.getDescriptionId()).getString();
            int amplifier = effect.getAmplifier();
            if (amplifier > 0) {
                name += " " + roman(amplifier + 1);
            }

            if (showDuration.getValue()) {
                name += "  " + duration(effect);
            }

            lines.add(name);
        }

        float padding = 3.0F;
        float textHeight = FontUtil.getHeight();

        float maxWidth = 0.0F;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, FontUtil.getStringWidth(line));
        }

        this.width = maxWidth + padding * 2.0F;
        this.height = textHeight * lines.size() + padding * 2.0F;

        if (background.getValue()) {
            RenderUtil.drawRect(context, renderX, renderY, this.width, this.height, new Color(0, 0, 0, 120).getRGB());
        }

        float y = renderY + padding;
        for (int index = 0; index < lines.size(); index++) {
            int color = effects.get(index).isInfiniteDuration() ? 0xFFFFC857 : 0xFFD8D8D8;
            FontUtil.drawStringWithShadow(context, lines.get(index), renderX + padding, y, color);
            y += textHeight;
        }
    }

    private String duration(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) return "*";

        int totalSeconds = effect.getDuration() / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return minutes > 0 ? String.format("%d:%02d", minutes, seconds) : String.valueOf(seconds);
    }

    private String roman(int value) {
        return value < ROMAN.length ? ROMAN[value] : String.valueOf(value);
    }
}
