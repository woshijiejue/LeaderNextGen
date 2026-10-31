package cn.leader.module.impl.visual;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.RenderSkiaEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.module.value.impl.ModeValue;
import cn.leader.module.value.impl.NumberValue;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.util.player.EntityUtil;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.RenderUtil;
import cn.leader.util.render.SkiaRender;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.skija.Canvas;

public final class ESP extends Module {
    public final ModeValue boxMode = new ModeValue("Box", "Corners", "None", "Full", "Corners");
    public final BoolValue healthBar = new BoolValue("Health Bar", true);
    public final BoolValue armorBar = new BoolValue("Armor Bar", true);
    public final BoolValue nametags = new BoolValue("Name Tags", true);
    public final BoolValue boxFill = new BoolValue("Box Fill", false);
    public final NumberValue fillAlpha = new NumberValue("Fill Opacity", 0.12F, 0.02F, 0.3F, 0.01F, boxFill::getValue);

    public ESP() {
        super("ESP", Category.Visual);
        setDescription("ESP");
    }

    @EventTarget
    public void onRenderSkia(RenderSkiaEvent event) {
        if (event.getLayer() != RenderSkiaEvent.Layer.HUD) return;
        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) return;

        for (Entity entity : instance.getTargetManager().getTargets()) {
            if (entity.equals(mc.player) && mc.options.getCameraType().isFirstPerson()) continue;

            if (EntityUtil.isSelected(entity)) {
                draw(event.getCanvas(), entity);
            }
        }
    }

    private void draw(Canvas canvas, @NotNull Entity entity) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean any = false;

        for (Vec3 corner : RenderUtil.getVectors(entity)) {
            Vec3 screen = RenderUtil.worldSpaceToScreenSpace(corner);
            if (screen.z <= 0 || screen.z >= 1) return;
            minX = Math.min(minX, (float) screen.x);
            minY = Math.min(minY, (float) screen.y);
            maxX = Math.max(maxX, (float) screen.x);
            maxY = Math.max(maxY, (float) screen.y);
            any = true;
        }

        if (!any || maxX - minX < 1 || maxY - minY < 1) return;

        int first = PanelRenderer.accent(0);
        int second = PanelRenderer.accent(6);
        float w = maxX - minX;
        float h = maxY - minY;

        if (boxFill.getValue() && !boxMode.is("None")) {
            canvas.save();
            SkiaRender.clipRoundRect(canvas, minX, minY, w, h, 3.0F);
            SkiaRender.verticalGradient(canvas, minX, minY, w, h, 3.0F,
                    SkiaRender.alpha(first, fillAlpha.getValue()),
                    SkiaRender.alpha(second, fillAlpha.getValue() * 0.25F));
            canvas.restore();
        }

        if (boxMode.is("Full")) {
            SkiaRender.roundRectOutline(canvas, minX - 0.5F, minY - 0.5F, w + 1.0F, h + 1.0F, 2, 2.5F, 0xA0000000);
            SkiaRender.gradientOutline(canvas, minX, minY, w, h, 2, 1.0F, first, second);
        } else if (boxMode.is("Corners")) {
            float len = Math.min(Math.min(w, h) / 3.5F, 12.0F);
            corners(canvas, minX, minY, maxX, maxY, len, 3.0F, 0xB0000000);
            corners(canvas, minX, minY, maxX, maxY, len, 1.75F, 0xFFFFFFFF);
            corners(canvas, minX, minY, maxX, maxY, len, 1.0F, first);
        }

        if (entity instanceof LivingEntity living) {
            if (healthBar.getValue() && living.getHealth() > 0) {
                float ratio = Math.max(0, Math.min(1, living.getHealth() / Math.max(1.0F, living.getMaxHealth())));
                int color = java.awt.Color.HSBtoRGB(ratio * 0.33F, 0.78F, 0.98F) | 0xFF000000;
                float barX = minX - 5.0F;
                SkiaRender.roundRect(canvas, barX, minY, 3.0F, h, 1.5F, 0xA0000000);
                if (ratio > 0) {
                    SkiaRender.roundRect(canvas, barX + 0.75F, minY + h * (1 - ratio), 1.5F,
                            h * ratio, 0.75F, color);
                }
            }

            if (armorBar.getValue() && living.getArmorValue() > 0) {
                float ratio = Math.max(0, Math.min(1, living.getArmorValue() / 20.0F));
                float barX = maxX + 2.0F;
                SkiaRender.roundRect(canvas, barX, minY, 3.0F, h, 1.5F, 0xA0000000);
                if (ratio > 0) {
                    SkiaRender.roundRect(canvas, barX + 0.75F, minY + h * (1 - ratio), 1.5F,
                            h * ratio, 0.75F, 0xFF72C8FF);
                }
            }

            if (nametags.getValue()) {
                nametag(canvas, living, minX + w / 2, minY);
            }
        }
    }

    private void corners(Canvas canvas, float x1, float y1, float x2, float y2, float len, float width, int color) {
        SkiaRender.line(canvas, x1, y1, x1 + len, y1, width, color);
        SkiaRender.line(canvas, x1, y1, x1, y1 + len, width, color);
        SkiaRender.line(canvas, x2, y1, x2 - len, y1, width, color);
        SkiaRender.line(canvas, x2, y1, x2, y1 + len, width, color);
        SkiaRender.line(canvas, x1, y2, x1 + len, y2, width, color);
        SkiaRender.line(canvas, x1, y2, x1, y2 - len, width, color);
        SkiaRender.line(canvas, x2, y2, x2 - len, y2, width, color);
        SkiaRender.line(canvas, x2, y2, x2, y2 - len, width, color);
    }

    private void nametag(Canvas canvas, LivingEntity living, float centerX, float top) {
        SkiaFont nameFont = Fonts.medium(8);
        SkiaFont numberFont = Fonts.number(7.5F);

        String name = living.getName().getString();
        String health = String.format(java.util.Locale.ROOT, "%.1f", Math.max(0.0F, living.getHealth()));
        String distance = String.format(java.util.Locale.ROOT, "%.1fm", mc.player.distanceTo(living));

        name = trim(nameFont, name, 118.0F);
        float nameWidth = nameFont.getStringWidth(name);
        float healthWidth = numberFont.getStringWidth(health);
        float distanceWidth = numberFont.getStringWidth(distance);

        float width = nameWidth + healthWidth + distanceWidth + 22;
        float height = 17;
        float x = centerX - width / 2;
        float y = top - height - 4;
        float centerY = y + height / 2;

        PanelRenderer.panel(canvas, x, y, width, height, height / 2, 1.0F, 0);

        float textX = x + 6;
        numberFont.drawStringCenteredY(canvas, distance, textX, centerY, 0xFFA8A8B3, false);
        textX += distanceWidth + 5;
        SkiaRender.line(canvas, textX - 2, centerY - 4, textX - 2, centerY + 4, 0.5F, 0x40FFFFFF);
        nameFont.drawStringCenteredY(canvas, name, textX, centerY, 0xFFFFFFFF, false);
        textX += nameWidth + 5;

        float ratio = Math.max(0, Math.min(1, living.getHealth() / Math.max(1.0F, living.getMaxHealth())));
        int color = java.awt.Color.HSBtoRGB(ratio * 0.33F, 0.65F, 1.0F) | 0xFF000000;
        numberFont.drawStringCenteredY(canvas, health, textX, centerY, color, false);
        SkiaRender.roundRect(canvas, x + 5, y + height - 2.5F, width - 10, 1.5F, 0.75F, 0x35FFFFFF);
        if (ratio > 0) {
            SkiaRender.roundRect(canvas, x + 5, y + height - 2.5F, (width - 10) * ratio, 1.5F, 0.75F, color);
        }
    }

    private static String trim(SkiaFont font, String text, float maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        String trimmed = text;
        while (!trimmed.isEmpty() && font.getStringWidth(trimmed + "…") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.offsetByCodePoints(trimmed.length(), -1));
        }
        return trimmed + "…";
    }
}
