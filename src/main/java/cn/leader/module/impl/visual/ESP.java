package cn.leader.module.impl.visual;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.Render2DEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.util.player.EntityUtil;
import cn.leader.util.render.ColorUtil;
import cn.leader.util.render.FontUtil;
import cn.leader.util.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4d;

import java.awt.*;

public final class ESP extends Module {
    public final BoolValue boxEsp = new BoolValue("Box", true);
    public final BoolValue healthBar = new BoolValue("Health Bar", true);
    public final BoolValue armorBar = new BoolValue("Armor Bar", true);
    public final BoolValue nametags = new BoolValue("Name Tags", true);

    public ESP() {
        super("ESP", Category.Visual);
        setDescription("ESP");
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (mc.player == null || mc.world == null) return;

        for (Entity entity : instance.getTargetManager().getTargets()) {
            if (entity.equals(mc.player) && mc.options.getPerspective().isFirstPerson()) continue;

            if (EntityUtil.isSelected(entity)) {
                drawBox(entity, e.getContext());
            }
        }
    }

    private void drawBox(@NotNull Entity entity, DrawContext context) {
        Vec3d[] vectors = RenderUtil.getVectors(entity);

        Vector4d position = null;
        for (Vec3d vector : vectors) {
            vector = RenderUtil.worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
            if (vector.z > 0 && vector.z < 1) {
                if (position == null) {
                    position = new Vector4d(vector.x, vector.y, vector.z, 0);
                }

                position.x = Math.min(vector.x, position.x);
                position.y = Math.min(vector.y, position.y);
                position.z = Math.max(vector.x, position.z);
                position.w = Math.max(vector.y, position.w);
            }
        }

        if (position != null) {
            float x = (float) position.x;
            float y = (float) position.y;
            float right = (float) position.z;
            float bottom = (float) position.w;

            if (boxEsp.getValue()) {
                drawBox(context, x, y, right, bottom);
            }

            if (entity instanceof LivingEntity living) {
                if (living.getHealth() != 0 && healthBar.getValue()) {
                    drawHealthBar(context, living, x, y, bottom);
                }

                if (living.getArmor() != 0 && armorBar.getValue()) {
                    drawArmorBar(context, living, right, y, bottom);
                }
            }

            if (nametags.getValue()) {
                drawNametag(context, entity, x, y, right);
            }
        }
    }

    private void drawNametag(DrawContext context, Entity entity, float x, float y, float right) {
        if (mc.player == null || mc.world == null) return;

        String name = "";
        String health = "";
        String distance = "";

        if (entity instanceof LivingEntity living) {
            name = living.getName().getString();
            health = String.format(" §7[§a%.1f§7]", living.getHealth());
            distance = String.format(" §f%dm", (int) mc.player.distanceTo(living));
        }

        String text = name + health + distance;
        float width = FontUtil.getStringWidth(name + health + distance);
        float height = FontUtil.getHeight();
        float middle = x + (right - x) / 2;

        RenderUtil.drawRect(context, middle - (width / 2.0F) - 2.0F, y - height - 4.0F, width + 4.0F, height + 2.0F, new Color(0, 0, 0, 120).getRGB());
        FontUtil.drawString(context, text, middle - (width / 2.0F), y - height - 3.0F, -1, true);
    }

    private void drawBox(DrawContext context, float x, float y, float right, float bottom) {
        HUD hud = getModule(HUD.class);

        int color1 = hud.getColor(1);
        int color2 = hud.getColor(2);
        int color3 = hud.getColor(3);
        int color4 = hud.getColor(4);
        int black = 0xFF000000;
        float t = 0.5f;

        RenderUtil.drawGradientRect(context, x, y, (right - x), 1.0f, color1, color2, true); // 顶
        RenderUtil.drawGradientRect(context, x, y, 1.0f, (bottom - y), color1, color4, false); // 左
        RenderUtil.drawGradientRect(context, x, bottom, (right - x), 1.0f, color4, color3, true); // 底
        RenderUtil.drawGradientRect(context, right, y, 1.0f, (bottom - y) + 1.0f, color2, color3, false); // 右

        // Outer Outline
        RenderUtil.drawRect(context, x - t, y - t, (right - x) + 1.0f + 2 * t, t, black); // 外顶
        RenderUtil.drawRect(context, x - t, bottom + 1.0f, (right - x) + 1.0f + 2 * t, t, black); // 外底
        RenderUtil.drawRect(context, x - t, y, t, (bottom - y) + 1.0f, black); // 外左
        RenderUtil.drawRect(context, right + 1.0f, y, t, (bottom - y) + 1.0f, black); // 外右

        // Inner Outline
        RenderUtil.drawRect(context, x + 1.0f, y + 1.0f, (right - x) - 1.0f, t, black); // 内顶
        RenderUtil.drawRect(context, x + 1.0f, bottom - t, (right - x) - 1.0f, t, black); // 内底
        RenderUtil.drawRect(context, x + 1.0f, y + 1.0f, t, (bottom - y) - 1.0f, black); // 内左
        RenderUtil.drawRect(context, right - t, y + 1.0f, t, (bottom - y) - 1.0f, black); // 内右
    }

    private void drawHealthBar(DrawContext context, @NotNull LivingEntity entity, float x, float y, float bottom) {
        float healthValue = entity.getHealth() / entity.getMaxHealth();
        float height = (bottom - y) + 1;

        RenderUtil.drawRect(context, x - 3.5f, y - 0.5f, 2, height + 1, new Color(0, 0, 0, 180).getRGB());
        RenderUtil.drawRect(context, x - 3.0f, y, 1, height, ColorUtil.applyAlpha(getHealthColor(healthValue * 100).getRGB(), 0.3F));
        RenderUtil.drawRect(context, x - 3.0f, y + (height - height * healthValue), 1, height * healthValue, getHealthColor(healthValue * 100).getRGB());
    }

    private void drawArmorBar(DrawContext context, @NotNull LivingEntity entity, float right, float y, float bottom) {
        float armorValue = entity.getArmor() / 20f;
        float height = (bottom - y) + 1;

        RenderUtil.drawRect(context, right + 1.5f, y - 0.5f, 2, height + 1, new Color(0, 0, 0, 180).getRGB());
        RenderUtil.drawRect(context, right + 2, y, 1, height, ColorUtil.applyAlpha(new Color(135, 206, 250).getRGB(), 0.3F));
        RenderUtil.drawRect(context, right + 2, y + (height - height * armorValue), 1, height * armorValue, new Color(135, 206, 250).getRGB());
    }

    private Color getHealthColor(float healthPercent) {
        return healthPercent > 75 ? new Color(66, 246, 123) : healthPercent > 50 ? new Color(228, 255, 105) : healthPercent > 35 ? new Color(236, 100, 64) : new Color(255, 65, 68);
    }
}