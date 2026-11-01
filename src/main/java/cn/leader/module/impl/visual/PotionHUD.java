package cn.leader.module.impl.visual;

import cn.injection.accessor.GuiAccessor;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.skija.Canvas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PotionHUD extends Drag {
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private final BoolValue showDuration = new BoolValue("Duration", true);
    private final BoolValue progressBar = new BoolValue("Progress Bar", false);
    private final BoolValue background = new BoolValue("Background", true);
    private final BoolValue sortByDuration = new BoolValue("Sort By Duration", true);
    private final BoolValue showHeader = new BoolValue("Header", false);
    private final Map<Holder<MobEffect>, Integer> maxDurations = new HashMap<>();

    public PotionHUD() {
        super("PotionHUD");
        setDescription("Active status effects");
        percentX = 0.01F;
        percentY = 0.3F;
    }

    @Override
    public void render(Canvas canvas) {
        if (mc.player == null) {
            maxDurations.clear();
            width = 0;
            height = 0;
            return;
        }

        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffects());
        maxDurations.keySet().removeIf(effect -> effects.stream().noneMatch(e -> e.getEffect().equals(effect)));
        if (effects.isEmpty()) {
            if (mc.gui.screen() instanceof ChatScreen) {
                width = 104;
                height = 18;
                drawBackground(canvas);
                Fonts.regular(8.0F).drawStringCenteredY(canvas, "No effects", renderX + 7.0F,
                        renderY + height / 2.0F, 0xFFAAAAB3, false);
                return;
            }
            width = 0;
            height = 0;
            return;
        }

        if (sortByDuration.getValue()) {
            effects.sort(Comparator.comparingInt((MobEffectInstance effect) ->
                    effect.isInfiniteDuration() ? Integer.MAX_VALUE : effect.getDuration())
                    .reversed().thenComparing(MobEffectInstance::getDescriptionId));
        }

        SkiaFont nameFont = Fonts.regular(8.0F);
        SkiaFont timeFont = Fonts.number(7.5F);
        float padding = 6.0F;
        float headerHeight = showHeader.getValue() ? 15.0F : 0.0F;
        float rowHeight = progressBar.getValue() ? 24.0F : 20.0F;
        float nameWidth = 0.0F;
        float timeWidth = 0.0F;
        for (MobEffectInstance effect : effects) {
            nameWidth = Math.max(nameWidth, nameFont.getStringWidth(name(effect)));
            if (showDuration.getValue()) timeWidth = Math.max(timeWidth, timeFont.getStringWidth(duration(effect)));
        }

        width = Math.max(138.0F, padding * 2.0F + 22.0F + nameWidth + timeWidth + 10.0F);
        height = padding * 2.0F + headerHeight + rowHeight * effects.size();
        drawBackground(canvas);

        // Text/background use Skia; vanilla effect icons are queued into the GUI
        // render state and drawn after the Skia HUD, like the TargetHUD skin.
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc,
                ((GuiAccessor) (Object) mc.gui).clientbase$getGuiRenderState(),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());

        if (showHeader.getValue()) {
            Fonts.medium(7.0F).drawStringCenteredY(canvas, "EFFECTS", renderX + padding,
                    renderY + padding + headerHeight / 2.0F, SkiaRender.alpha(PanelRenderer.accent(0), 1.0F), false);
            SkiaRender.line(canvas, renderX + padding + 35.0F, renderY + padding + headerHeight - 1.0F,
                    renderX + width - padding, renderY + padding + headerHeight - 1.0F,
                    0.6F, 0x28FFFFFF);
        }

        float y = renderY + padding + headerHeight;
        for (int index = 0; index < effects.size(); index++) {
            MobEffectInstance effect = effects.get(index);
            int color = effect.getEffect().value().getColor() | 0xFF000000;
            float centerY = y + rowHeight / 2.0F;

            String effectName = name(effect);
            Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            if (effectId != null) {
                Identifier texture = Identifier.fromNamespaceAndPath(effectId.getNamespace(),
                        "textures/mob_effect/" + effectId.getPath() + ".png");
                // The vanilla icon is 18x18; scale the entire texture to 16x16.
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                        Math.round(renderX + padding), Math.round(centerY - 8.0F),
                        0.0F, 0.0F, 16, 16, 18, 18, 18, 18, 0xFFFFFFFF);
            }
            float nameX = renderX + padding + 22.0F;
            float timeSpace = showDuration.getValue() ? timeWidth + 5.0F : 0.0F;
            nameFont.drawStringCenteredY(canvas, trim(nameFont, effectName,
                            Math.max(20.0F, width - padding - (nameX - renderX) - timeSpace)),
                    nameX, centerY, effect.isInfiniteDuration() ? 0xFFFFD27A : 0xFFE4E4E8, false);

            if (showDuration.getValue()) {
                String time = duration(effect);
                int timeColor = effect.isInfiniteDuration() ? 0xFFFFD27A
                        : effect.getDuration() <= 100 ? 0xFFFF9999 : 0xFFA9A9B2;
                timeFont.drawStringCenteredY(canvas, time,
                        renderX + width - padding - timeFont.getStringWidth(time), centerY, timeColor, false);
            }

            if (progressBar.getValue()) {
                int max = maxDurations.merge(effect.getEffect(), Math.max(1, effect.getDuration()), Math::max);
                float ratio = effect.isInfiniteDuration() ? 1.0F
                        : Math.min(1.0F, effect.getDuration() / (float) Math.max(1, max));
                float barX = renderX + padding + 22.0F;
                float barWidth = width - padding * 2.0F - 22.0F;
                SkiaRender.roundRect(canvas, barX, y + rowHeight - 3.0F, barWidth, 1.0F, 0.5F, 0x35FFFFFF);
                if (ratio > 0.0F) {
                    SkiaRender.roundRect(canvas, barX, y + rowHeight - 3.0F,
                            barWidth * ratio, 1.0F, 0.5F, SkiaRender.alpha(color, 0.9F));
                }
            }
            y += rowHeight;
        }
    }

    private void drawBackground(Canvas canvas) {
        if (!background.getValue()) return;
        int accent = PanelRenderer.accent(0);
        PanelRenderer.panel(canvas, renderX, renderY, width, height,
                Math.min(5.0F, PanelRenderer.radius()), 1.0F, accent, true);
        SkiaRender.horizontalGradient(canvas, renderX, renderY, width, 1.5F, 0,
                SkiaRender.alpha(accent, 0.9F), SkiaRender.alpha(PanelRenderer.accent(5), 0.9F));
    }

    private static String trim(SkiaFont font, String text, float maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        String result = text;
        while (result.length() > 1 && font.getStringWidth(result + "...") > maxWidth) {
            result = result.substring(0, result.offsetByCodePoints(result.length(), -1));
        }
        return result + "...";
    }

    private String name(MobEffectInstance effect) {
        String name = Component.translatable(effect.getDescriptionId()).getString();
        int amplifier = effect.getAmplifier();
        return amplifier > 0 ? name + " " + roman(amplifier + 1) : name;
    }

    private String duration(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) return "INF";
        int totalSeconds = effect.getDuration() / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private String roman(int value) {
        return value < ROMAN.length ? ROMAN[value] : String.valueOf(value);
    }
}
