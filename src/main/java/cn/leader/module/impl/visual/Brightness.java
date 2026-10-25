package cn.leader.module.impl.visual;

import cn.leader.event.base.annotation.EventTarget;
import cn.leader.event.impl.UpdateEvent;
import cn.leader.module.Category;
import cn.leader.module.Module;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Brightness extends Module {

    public Brightness() {
        super("Brightness", Category.Visual);
        setDescription("The screen is highlighted");
    }

    @Override
    public void onDisable() {
        if (mc.player == null) return;

        mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 8963, 0, false, false, false));
    }
}