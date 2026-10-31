package cn.leader.module.impl.visual;

import cn.injection.accessor.GuiAccessor;
import cn.leader.module.impl.combat.KillAura;
import cn.leader.module.value.impl.BoolValue;
import cn.leader.ui.font.base.SkiaFont;
import cn.leader.ui.hud.Drag;
import cn.leader.util.animation.Easing;
import cn.leader.util.animation.EasingAnimation;
import cn.leader.util.render.Fonts;
import cn.leader.util.render.PanelRenderer;
import cn.leader.util.render.SkiaRender;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.skija.Canvas;

public final class TargetHUD extends Drag {
    private final BoolValue healthBar = new BoolValue("Health Bar", true);
    private final BoolValue showDistance = new BoolValue("Distance", true);
    private final BoolValue showArmor = new BoolValue("Armor", true);
    private final BoolValue showAbsorption = new BoolValue("Absorption", true);
    private final BoolValue showEquipment = new BoolValue("Equipment Slots", true);
    private final BoolValue background = new BoolValue("Background", true);
    private final BoolValue showHeader = new BoolValue("Header", false);

    private final EasingAnimation healthAnimation = new EasingAnimation(Easing.EASE_OUT_QUART, 300);
    private final EasingAnimation fadeAnimation = new EasingAnimation(Easing.EASE_OUT_QUART, 200);
    private LivingEntity lastTarget;

    public TargetHUD() {
        super("TargetHUD");
        setDescription("Target information overlay");
        percentX = 0.55F;
        percentY = 0.6F;
        setEnabled(true);
    }

    @Override
    public void render(Canvas canvas) {
        if (mc.player == null || mc.level == null) {
            width = 0;
            height = 0;
            return;
        }

        LivingEntity target = getModule(KillAura.class).getTarget();
        if (target == null && mc.gui.screen() instanceof ChatScreen) target = mc.player;

        boolean visible = target != null && target.isAlive();
        if (visible) {
            if (lastTarget != target) {
                healthAnimation.setValue(target.getHealth() / Math.max(1.0F, target.getMaxHealth()));
            }
            lastTarget = target;
        }
        fadeAnimation.run(visible ? 1.0F : 0.0F);
        float alpha = (float) fadeAnimation.getValue();
        if (lastTarget == null || alpha <= 0.02F) {
            width = 0;
            height = 0;
            return;
        }

        LivingEntity entity = lastTarget;
        SkiaFont nameFont = Fonts.medium(9.0F);
        SkiaFont infoFont = Fonts.regular(7.5F);
        SkiaFont numberFont = Fonts.number(7.5F);

        float padding = 6.0F;
        float avatar = 22.0F;
        float top = showHeader.getValue() ? 16.0F : padding;
        float equipmentWidth = showEquipment.getValue() ? 76.0F : 0.0F;
        width = showEquipment.getValue() ? 178.0F : 145.0F;
        height = top + avatar + (healthBar.getValue() ? 7.0F : 0.0F) + padding;

        int accent = PanelRenderer.accent(0);
        canvas.save();
        if (background.getValue()) {
            PanelRenderer.panel(canvas, renderX, renderY, width, height,
                    Math.min(5.0F, PanelRenderer.radius()), alpha, accent, true);
        }

        SkiaRender.roundRect(canvas, renderX, renderY + 5.0F, 2.0F,
                Math.max(10.0F, height - 10.0F), 1.0F, SkiaRender.alpha(accent, alpha));

        if (showHeader.getValue()) {
            Fonts.medium(6.5F).drawStringCenteredY(canvas, "TARGET", renderX + padding,
                    renderY + 8.0F, SkiaRender.alpha(accent, alpha), false);
            SkiaRender.line(canvas, renderX + padding + 30.0F, renderY + 8.0F,
                    renderX + width - padding, renderY + 8.0F, 0.6F,
                    SkiaRender.alpha(0x35FFFFFF, alpha));
        }

        float avatarX = renderX + padding;
        float avatarY = renderY + top;
        SkiaRender.roundRect(canvas, avatarX, avatarY, avatar, avatar, 4.0F,
                SkiaRender.alpha(0xFF121217, 0.9F * alpha));
        SkiaRender.roundRectOutline(canvas, avatarX, avatarY, avatar, avatar, 4.0F, 0.5F,
                SkiaRender.alpha(accent, 0.55F * alpha));

        String name = entity.getName().getString();
        String initial = name.isEmpty() ? "?" : name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(java.util.Locale.ROOT);
        SkiaFont initialFont = Fonts.bold(12.0F);
        initialFont.drawStringCenteredY(canvas, initial,
                avatarX + avatar / 2.0F - initialFont.getStringWidth(initial) / 2.0F,
                avatarY + avatar / 2.0F, SkiaRender.alpha(0xFFF0F0F2, alpha), false);

        float textX = avatarX + avatar + 6.0F;
        float equipmentX = renderX + width - padding - 70.0F;
        float textRight = showEquipment.getValue() ? equipmentX - 6.0F : renderX + width - padding;
        String distance = showDistance.getValue()
                ? String.format(java.util.Locale.ROOT, "%.1fm", mc.player.distanceTo(entity)) : "";
        float distanceWidth = distance.isEmpty() ? 0.0F : numberFont.getStringWidth(distance) + 5.0F;

        nameFont.drawStringCenteredY(canvas,
                trim(nameFont, name, Math.max(18.0F, textRight - textX - distanceWidth)), textX,
                avatarY + 7.0F, SkiaRender.alpha(0xFFF0F0F2, alpha), false);
        if (!distance.isEmpty()) {
            numberFont.drawStringCenteredY(canvas, distance,
                    textRight - numberFont.getStringWidth(distance), avatarY + 7.0F,
                    SkiaRender.alpha(0xFFA9A9B2, alpha), false);
        }

        float health = Math.max(0.0F, entity.getHealth());
        String info = String.format(java.util.Locale.ROOT, "%.1f HP", health);
        if (showArmor.getValue()) info += "  ·  " + entity.getArmorValue() + " armor";
        infoFont.drawStringCenteredY(canvas, trim(infoFont, info, Math.max(18.0F, textRight - textX)),
                textX, avatarY + 16.5F, SkiaRender.alpha(0xFFB9BAC2, alpha), false);

        if (healthBar.getValue()) {
            float barY = avatarY + avatar + 3.0F;
            float barWidth = Math.max(10.0F, textRight - textX);
            float ratio = Math.max(0.0F, Math.min(1.0F, health / Math.max(1.0F, entity.getMaxHealth())));
            healthAnimation.run(ratio);
            float animated = Math.max(0.0F, Math.min(1.0F, (float) healthAnimation.getValue()));
            SkiaRender.roundRect(canvas, textX, barY, barWidth, 2.0F, 1.0F,
                    SkiaRender.alpha(0x55FFFFFF, alpha));
            if (animated > 0.0F) {
                SkiaRender.horizontalGradient(canvas, textX, barY, barWidth * animated, 2.0F, 1.0F,
                        SkiaRender.alpha(accent, alpha), SkiaRender.alpha(PanelRenderer.accent(5), alpha));
            }
            if (showAbsorption.getValue() && entity.getAbsorptionAmount() > 0.0F) {
                float absorption = Math.min(1.0F, entity.getAbsorptionAmount()
                        / Math.max(1.0F, entity.getMaxHealth()));
                SkiaRender.roundRect(canvas, textRight - barWidth * absorption, barY + 3.0F,
                        barWidth * absorption, 1.0F, 0.5F, SkiaRender.alpha(0xFFFFC94D, alpha));
            }
        }

        if (showEquipment.getValue()) drawEquipmentSlots(entity, canvas, equipmentX, renderY + top + 3.0F, alpha);
        canvas.restore();
    }

    private void drawEquipmentSlots(LivingEntity entity, Canvas canvas, float x, float y, float alpha) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc,
                ((GuiAccessor) (Object) mc.gui).clientbase$getGuiRenderState(), guiWidth, guiHeight);

        for (int i = 0; i < slots.length; i++) {
            float slotX = x + i * 18.0F;
            SkiaRender.roundRect(canvas, slotX, y, 16.0F, 16.0F, 3.0F,
                    SkiaRender.alpha(0xFF08090C, 0.25F * alpha));
            SkiaRender.roundRectOutline(canvas, slotX, y, 16.0F, 16.0F, 3.0F, 0.5F,
                    SkiaRender.alpha(0xFFFFFFFF, 0.10F * alpha));
            ItemStack item = entity.getItemBySlot(slots[i]);
            if (!item.isEmpty()) {
                int itemX = Math.round(slotX);
                int itemY = Math.round(y);
                graphics.item(item, itemX, itemY);
                graphics.itemDecorations(mc.font, item, itemX, itemY);
            }
        }
    }

    private static String trim(SkiaFont font, String text, float maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        String result = text;
        while (result.length() > 1 && font.getStringWidth(result + "...") > maxWidth) {
            result = result.substring(0, result.offsetByCodePoints(result.length(), -1));
        }
        return result + "...";
    }
}
