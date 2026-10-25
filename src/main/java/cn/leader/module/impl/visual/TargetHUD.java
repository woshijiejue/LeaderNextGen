package cn.leader.module.impl.visual;

import cn.leader.module.impl.combat.KillAura;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.ui.hud.Drag;
import cn.leader.util.render.FontUtil;
import cn.leader.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;

import java.awt.*;

/**
 * Live information about whatever the aura is currently looking at.
 */
public final class TargetHUD extends Drag {

    private final BoolValue healthBar = new BoolValue("Health Bar", true);
    private final BoolValue showDistance = new BoolValue("Distance", true);
    private final BoolValue showArmor = new BoolValue("Armor", false);
    private final BoolValue showAbsorption = new BoolValue("Absorption", true);
    private final BoolValue background = new BoolValue("Background", true);

    public TargetHUD() {
        super("TargetHUD");
        setDescription("Target information overlay");
        percentX = 0.02F;
        percentY = 0.45F;
        setEnabled(true);
    }

    @Override
    public void render(GuiGraphicsExtractor context) {
        if (mc.player == null || mc.world == null || mc.font == null) {
            this.width = 0;
            this.height = 0;
            return;
        }

        LivingEntity target = getModule(KillAura.class).getTarget();
        if (target == null || !target.isAlive()) {
            this.width = 0;
            this.height = 0;
            return;
        }

        HUD hud = getModule(HUD.class);

        String name = target.getName().getString();
        String distance = String.format("%.1fm", mc.player.distanceTo(target));

        float padding = 4.0F;
        float textHeight = FontUtil.getHeight();
        float barHeight = healthBar.getValue() ? 3.0F : 0.0F;
        float extraHeight = showArmor.getValue() ? textHeight + 2.0F : 0.0F;

        float headerWidth = FontUtil.getStringWidth(name);
        if (showDistance.getValue()) {
            headerWidth += FontUtil.getStringWidth(distance) + 10.0F;
        }

        this.width = Math.max(72.0F, headerWidth) + padding * 2.0F + 2.0F;
        this.height = padding * 2.0F + textHeight
                + (barHeight > 0.0F ? barHeight + 3.0F : 0.0F)
                + extraHeight;

        if (background.getValue()) {
            RenderUtil.drawRect(context, renderX, renderY, this.width, this.height, new Color(0, 0, 0, 120).getRGB());
        }

        RenderUtil.drawRect(context, renderX, renderY, 2.0F, this.height, hud.getColor(1));

        float contentX = renderX + padding + 2.0F;
        float contentY = renderY + padding;

        FontUtil.drawStringWithShadow(context, name, contentX, contentY, -1);

        if (showDistance.getValue()) {
            FontUtil.drawStringWithShadow(context, distance,
                    renderX + this.width - padding - FontUtil.getStringWidth(distance), contentY, 0xFFB0B0B0);
        }

        contentY += textHeight + 2.0F;

        if (barHeight > 0.0F) {
            float barWidth = this.width - (padding + 2.0F) * 2.0F;
            float health = target.getHealth() + (showAbsorption.getValue() ? target.getAbsorptionAmount() : 0.0F);
            float ratio = Math.max(0.0F, Math.min(1.0F, health / Math.max(1.0F, target.getMaxHealth())));

            RenderUtil.drawRect(context, contentX, contentY, barWidth, barHeight, new Color(0, 0, 0, 160).getRGB());

            if (ratio > 0.0F) {
                int left = Color.HSBtoRGB(Math.min(0.33F, ratio * 0.33F), 0.72F, 0.95F) | 0xFF000000;
                int right = Color.HSBtoRGB(Math.max(0.0F, ratio * 0.33F - 0.06F), 0.82F, 0.85F) | 0xFF000000;
                RenderUtil.drawGradientRect(context, contentX, contentY, barWidth * ratio, barHeight, left, right, true);
            }

            contentY += barHeight + 3.0F;
        }

        if (showArmor.getValue()) {
            String armor = "Armor " + target.getArmorValue();
            FontUtil.drawStringWithShadow(context, armor, contentX, contentY, 0xFFB0B0B0);
        }
    }
}
